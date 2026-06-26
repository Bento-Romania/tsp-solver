package com.bento.tsp.controller;

import com.bento.tsp.model.Coordinate;
import com.bento.tsp.model.GeoJsonLineString;
import com.bento.tsp.model.MatrixResponse;
import com.bento.tsp.model.SolveRequest;
import com.bento.tsp.model.SolveResponse;
import com.bento.tsp.service.DistanceMatrixService;
import com.bento.tsp.service.GraphHopperService;
import com.bento.tsp.service.RouteGeometryService;
import com.bento.tsp.service.TspSolverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SolveController {

    private final DistanceMatrixService distanceMatrixService;
    private final TspSolverService tspSolverService;
    private final RouteGeometryService routeGeometryService;
    private final GraphHopperService graphHopperService;

    public SolveController(DistanceMatrixService distanceMatrixService,
                           TspSolverService tspSolverService,
                           RouteGeometryService routeGeometryService,
                           GraphHopperService graphHopperService) {
        this.distanceMatrixService = distanceMatrixService;
        this.tspSolverService = tspSolverService;
        this.routeGeometryService = routeGeometryService;
        this.graphHopperService = graphHopperService;
    }

    @PostMapping("/solve")
    public ResponseEntity<SolveResponse> solve(@Valid @RequestBody SolveRequest request) {
        if (!graphHopperService.isReady()) {
            return ResponseEntity.status(503).build();
        }

        List<Coordinate> coordinates = request.coordinates();
        int startIndex = request.startIndex();
        if (startIndex >= coordinates.size()) {
            return ResponseEntity.badRequest().build();
        }

        MatrixResponse matrix = distanceMatrixService.compute(coordinates);
        int[] order = tspSolverService.solve(matrix.distances(), startIndex);
        List<Coordinate> orderedCoordinates = Arrays.stream(order)
                .mapToObj(coordinates::get)
                .toList();
        GeoJsonLineString geometry = routeGeometryService.buildGeometry(coordinates, order);

        return ResponseEntity.ok(new SolveResponse(orderedCoordinates, geometry));
    }
}
