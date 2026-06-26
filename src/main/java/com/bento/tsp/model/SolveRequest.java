package com.bento.tsp.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SolveRequest(
        @NotNull @Size(min = 2, max = 100) List<Coordinate> coordinates,
        @NotNull @Min(0) Integer startIndex
) {}
