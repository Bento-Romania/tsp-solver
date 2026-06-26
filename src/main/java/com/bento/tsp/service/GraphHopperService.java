package com.bento.tsp.service;

import com.bento.tsp.config.GraphHopperProperties;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
public class GraphHopperService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GraphHopperService.class);

    private final GraphHopperProperties props;
    private final OsmDownloadService osmDownloadService;
    private volatile GraphHopper graphHopper;
    private volatile Exception initError;

    public GraphHopperService(GraphHopperProperties props, OsmDownloadService osmDownloadService) {
        this.props = props;
        this.osmDownloadService = osmDownloadService;
    }

    @Override
    public void afterPropertiesSet() {
        Thread thread = new Thread(this::initialize, "gh-init");
        thread.setDaemon(false);
        thread.start();
    }

    private void initialize() {
        try {
            Path graphPath = Path.of(props.graphLocation());
            Path osmPath = Path.of(props.osmFile());
            boolean graphCacheReady = Files.exists(graphPath.resolve("properties"));

            if (!graphCacheReady && !Files.exists(osmPath)) {
                osmDownloadService.download(props.osmDownloadUrl(), osmPath);
            }

            CustomModel carModel = new CustomModel()
                    .addToSpeed(Statement.If("!car_access", Statement.Op.MULTIPLY, "0"))
                    .addToSpeed(Statement.Else(Statement.Op.LIMIT, "car_average_speed"));

            GraphHopper hopper = new GraphHopper();
            hopper.setOSMFile(osmPath.toString());
            hopper.setGraphHopperLocation(props.graphLocation());
            hopper.setEncodedValuesString("car_access, car_average_speed");
            hopper.setProfiles(List.of(
                    new Profile(props.profile()).setWeighting("custom").setCustomModel(carModel)));
            hopper.getCHPreparationHandler().setCHProfiles(List.of(new CHProfile(props.profile())));

            if (graphCacheReady) {
                log.info("Loading existing graph cache from {}", graphPath);
            } else {
                log.info("Importing OSM data from {} — this may take several minutes", osmPath);
            }
            hopper.importOrLoad();

            this.graphHopper = hopper;
            log.info("GraphHopper ready.");
        } catch (Exception e) {
            log.error("GraphHopper initialization failed", e);
            this.initError = e;
        }
    }

    @Override
    public void destroy() {
        if (graphHopper != null) {
            graphHopper.close();
        }
    }

    public boolean isReady() {
        return graphHopper != null;
    }

    public Exception getInitError() {
        return initError;
    }

    public GHResponse route(double fromLat, double fromLon, double toLat, double toLon) {
        GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon)
                .setProfile(props.profile())
                .setLocale(Locale.US)
                .putHint("instructions", false)
                .putHint("calcPoints", false);
        return graphHopper.route(request);
    }
}
