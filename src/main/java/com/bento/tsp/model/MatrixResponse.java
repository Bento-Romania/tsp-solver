package com.bento.tsp.model;

public record MatrixResponse(
        double[][] distances,
        double[][] durations
) {}
