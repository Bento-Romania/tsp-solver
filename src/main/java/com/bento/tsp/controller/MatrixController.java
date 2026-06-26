package com.bento.tsp.controller;

import com.bento.tsp.model.MatrixRequest;
import com.bento.tsp.model.MatrixResponse;
import com.bento.tsp.service.DistanceMatrixService;
import com.bento.tsp.service.GraphHopperService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MatrixController {

    private final DistanceMatrixService distanceMatrixService;
    private final GraphHopperService graphHopperService;

    public MatrixController(DistanceMatrixService distanceMatrixService,
                             GraphHopperService graphHopperService) {
        this.distanceMatrixService = distanceMatrixService;
        this.graphHopperService = graphHopperService;
    }

    @PostMapping("/matrix")
    public ResponseEntity<MatrixResponse> computeMatrix(@Valid @RequestBody MatrixRequest request) {
        if (!graphHopperService.isReady()) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(distanceMatrixService.compute(request.coordinates()));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        if (graphHopperService.isReady()) {
            return ResponseEntity.ok("UP");
        }
        Exception err = graphHopperService.getInitError();
        if (err != null) {
            return ResponseEntity.status(500).body("FAILED: " + err.getMessage());
        }
        return ResponseEntity.status(503).body("STARTING");
    }
}
