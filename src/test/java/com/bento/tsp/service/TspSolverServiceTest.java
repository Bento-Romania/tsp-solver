package com.bento.tsp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TspSolverServiceTest {

    TspSolverService service;

    @BeforeEach
    void setUp() {
        service = new TspSolverService();
        service.init();
    }

    @Test
    void solveStartsFromGivenIndex() {
        double[][] distances = {
                {0, 1, 1},
                {1, 0, 1},
                {1, 1, 0}
        };

        int[] order = service.solve(distances, 2);

        assertThat(order[0]).isEqualTo(2);
        assertThat(order).hasSize(3);
    }

    @Test
    void solvePicksOptimalTour() {
        // Points arranged in a "chain": 0-1-2-3 with cheap consecutive edges,
        // expensive cross edges. Optimal closed tour is 0→1→2→3→0 or reverse.
        double[][] distances = {
                {0,   1, 100, 100},
                {1,   0,   1, 100},
                {100, 1,   0,   1},
                {100, 100, 1,   0}
        };

        int[] order = service.solve(distances, 0);

        assertThat(order).hasSize(4);
        assertThat(order[0]).isEqualTo(0);

        double totalCost = 0;
        for (int i = 0; i < order.length; i++) {
            int from = order[i];
            int to = order[(i + 1) % order.length];
            totalCost += distances[from][to];
        }
        assertThat(totalCost).isEqualTo(103.0);
    }

    @Test
    void solveContainsEveryNodeExactlyOnce() {
        double[][] distances = {
                {0,  10, 20, 30},
                {10,  0, 15, 25},
                {20, 15,  0,  5},
                {30, 25,  5,  0}
        };

        int[] order = service.solve(distances, 0);

        assertThat(order).hasSize(4);
        assertThat(order).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void solveRejectsStartIndexOutOfRange() {
        double[][] distances = {{0, 1}, {1, 0}};

        assertThatThrownBy(() -> service.solve(distances, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
