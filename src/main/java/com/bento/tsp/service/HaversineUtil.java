package com.bento.tsp.service;

import com.bento.tsp.model.Coordinate;

public final class HaversineUtil {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private HaversineUtil() {}

    public static double distanceMeters(Coordinate a, Coordinate b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }

    public static double durationSeconds(Coordinate a, Coordinate b, double speedKmh) {
        return distanceMeters(a, b) / (speedKmh / 3.6);
    }
}
