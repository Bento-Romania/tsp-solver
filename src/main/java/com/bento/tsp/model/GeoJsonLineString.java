package com.bento.tsp.model;

import java.util.List;

public record GeoJsonLineString(String type, List<double[]> coordinates) {

    public static GeoJsonLineString of(List<double[]> coordinates) {
        return new GeoJsonLineString("LineString", coordinates);
    }
}
