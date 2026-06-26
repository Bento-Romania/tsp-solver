package com.bento.tsp.service;

import com.bento.tsp.model.Coordinate;
import com.bento.tsp.model.GeoJsonLineString;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteGeometryService {

    private final GraphHopperService graphHopperService;

    public RouteGeometryService(GraphHopperService graphHopperService) {
        this.graphHopperService = graphHopperService;
    }

    public GeoJsonLineString buildGeometry(List<Coordinate> coordinates, int[] order) {
        List<GHPoint> waypoints = new ArrayList<>(order.length + 1);
        for (int idx : order) {
            Coordinate c = coordinates.get(idx);
            waypoints.add(new GHPoint(c.lat(), c.lon()));
        }
        // close the loop back to the start
        Coordinate start = coordinates.get(order[0]);
        waypoints.add(new GHPoint(start.lat(), start.lon()));

        PointList points = graphHopperService.routeOrdered(waypoints);

        List<double[]> coords;
        if (points.isEmpty()) {
            coords = straightLine(coordinates, order);
        } else {
            coords = new ArrayList<>(points.size());
            for (int i = 0; i < points.size(); i++) {
                coords.add(new double[]{points.getLon(i), points.getLat(i)});
            }
        }
        return GeoJsonLineString.of(coords);
    }

    private List<double[]> straightLine(List<Coordinate> coordinates, int[] order) {
        List<double[]> coords = new ArrayList<>(order.length + 1);
        for (int idx : order) {
            Coordinate c = coordinates.get(idx);
            coords.add(new double[]{c.lon(), c.lat()});
        }
        Coordinate start = coordinates.get(order[0]);
        coords.add(new double[]{start.lon(), start.lat()});
        return coords;
    }
}
