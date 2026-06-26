package com.bento.tsp.service;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class TspSolverService {

    @PostConstruct
    public void init() {
        Loader.loadNativeLibraries();
    }

    public int[] solve(double[][] distances, int startIndex) {
        int n = distances.length;
        if (startIndex < 0 || startIndex >= n) {
            throw new IllegalArgumentException("startIndex " + startIndex + " out of range [0, " + n + ")");
        }

        RoutingIndexManager manager = new RoutingIndexManager(n, 1, startIndex);
        RoutingModel routing = new RoutingModel(manager);

        int transitCallbackIndex = routing.registerTransitCallback((fromIndex, toIndex) -> {
            int from = manager.indexToNode(fromIndex);
            int to = manager.indexToNode(toIndex);
            return Math.round(distances[from][to]);
        });

        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .build();

        Assignment solution = routing.solveWithParameters(searchParameters);
        if (solution == null) {
            throw new IllegalStateException("OR-Tools could not find a TSP solution");
        }

        int[] order = new int[n];
        int i = 0;
        long index = routing.start(0);
        while (!routing.isEnd(index)) {
            order[i++] = manager.indexToNode(index);
            index = solution.value(routing.nextVar(index));
        }
        return order;
    }
}
