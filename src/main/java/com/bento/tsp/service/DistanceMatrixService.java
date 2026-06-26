package com.bento.tsp.service;

import com.bento.tsp.config.GraphHopperProperties;
import com.bento.tsp.model.Coordinate;
import com.bento.tsp.model.MatrixResponse;
import com.graphhopper.GHResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class DistanceMatrixService {

    private final GraphHopperService graphHopperService;
    private final GraphHopperProperties props;
    private final ExecutorService routingExecutor;

    public DistanceMatrixService(GraphHopperService graphHopperService,
                                  GraphHopperProperties props,
                                  ExecutorService routingExecutor) {
        this.graphHopperService = graphHopperService;
        this.props = props;
        this.routingExecutor = routingExecutor;
    }

    public MatrixResponse compute(List<Coordinate> coordinates) {
        int n = coordinates.size();
        double[][] distances = new double[n][n];
        double[][] durations = new double[n][n];

        List<CompletableFuture<Void>> futures = new ArrayList<>(n * (n - 1));

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                final int fi = i, fj = j;
                Coordinate from = coordinates.get(i);
                Coordinate to = coordinates.get(j);

                futures.add(CompletableFuture.runAsync(() -> {
                    double distM, durS;
                    try {
                        GHResponse response = graphHopperService.route(
                                from.lat(), from.lon(), to.lat(), to.lon());
                        if (response.hasErrors()) {
                            distM = HaversineUtil.distanceMeters(from, to);
                            durS = HaversineUtil.durationSeconds(from, to, props.haversineFallbackSpeedKmh());
                        } else {
                            distM = response.getBest().getDistance();
                            durS = response.getBest().getTime() / 1000.0;
                        }
                    } catch (Exception e) {
                        distM = HaversineUtil.distanceMeters(from, to);
                        durS = HaversineUtil.durationSeconds(from, to, props.haversineFallbackSpeedKmh());
                    }
                    distances[fi][fj] = distM;
                    durations[fi][fj] = durS;
                }, routingExecutor));
            }
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new MatrixResponse(distances, durations);
    }
}
