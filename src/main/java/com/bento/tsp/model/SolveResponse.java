package com.bento.tsp.model;

import java.util.List;

public record SolveResponse(
        List<Coordinate> orderedCoordinates,
        GeoJsonLineString geometry
) {}
