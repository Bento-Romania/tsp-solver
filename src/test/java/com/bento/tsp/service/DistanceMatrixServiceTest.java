package com.bento.tsp.service;

import com.bento.tsp.config.GraphHopperProperties;
import com.bento.tsp.model.Coordinate;
import com.bento.tsp.model.MatrixResponse;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistanceMatrixServiceTest {

    @Mock
    GraphHopperService graphHopperService;

    ExecutorService executor;
    DistanceMatrixService service;

    static final GraphHopperProperties PROPS = new GraphHopperProperties(
            "./data/graph-cache", "./data/romania-latest.osm.pbf",
            "https://download.geofabrik.de/europe/romania-latest.osm.pbf",
            4, "car", 20.0);

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        service = new DistanceMatrixService(graphHopperService, PROPS, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void returnsRoutedDistancesAndDurations() {
        GHResponse resp = successResponse(1000.0, 120_000L);
        when(graphHopperService.route(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(resp);

        MatrixResponse result = service.compute(List.of(
                new Coordinate(44.4268, 26.1025),
                new Coordinate(46.7712, 23.6236)));

        assertThat(result.distances()[0][1]).isEqualTo(1000.0);
        assertThat(result.distances()[1][0]).isEqualTo(1000.0);
        assertThat(result.durations()[0][1]).isEqualTo(120.0); // 120_000 ms → 120 s
        assertThat(result.durations()[1][0]).isEqualTo(120.0);
    }

    @Test
    void diagonalIsAlwaysZero() {
        GHResponse resp = successResponse(500.0, 60_000L);
        when(graphHopperService.route(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(resp);

        var coords = List.of(
                new Coordinate(44.4268, 26.1025),
                new Coordinate(46.7712, 23.6236),
                new Coordinate(45.7489, 21.2087));

        MatrixResponse result = service.compute(coords);

        for (int i = 0; i < 3; i++) {
            assertThat(result.distances()[i][i]).isEqualTo(0.0);
            assertThat(result.durations()[i][i]).isEqualTo(0.0);
        }
    }

    @Test
    void matrixDimensionsMatchCoordinateCount() {
        GHResponse resp = successResponse(100.0, 10_000L);
        when(graphHopperService.route(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(resp);

        int n = 5;
        var coords = List.of(
                new Coordinate(44.0, 26.0), new Coordinate(45.0, 25.0),
                new Coordinate(46.0, 24.0), new Coordinate(47.0, 23.0),
                new Coordinate(48.0, 22.0));

        MatrixResponse result = service.compute(coords);

        assertThat(result.distances()).hasNumberOfRows(n);
        assertThat(result.durations()).hasNumberOfRows(n);
        for (double[] row : result.distances()) assertThat(row).hasSize(n);
        for (double[] row : result.durations()) assertThat(row).hasSize(n);
    }

    @Test
    void fallsBackToHaversineWhenGHReturnsError() {
        GHResponse errResp = mock(GHResponse.class);
        when(errResp.hasErrors()).thenReturn(true);
        when(graphHopperService.route(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(errResp);

        var bucharest = new Coordinate(44.4268, 26.1025);
        var cluj = new Coordinate(46.7712, 23.6236);
        MatrixResponse result = service.compute(List.of(bucharest, cluj));

        // straight-line Bucharest→Cluj ~324 km
        assertThat(result.distances()[0][1]).isBetween(315_000.0, 335_000.0);
        // at 20 km/h: 324_000 / (20/3.6) ≈ 58_320 s
        assertThat(result.durations()[0][1]).isBetween(55_000.0, 62_000.0);
    }

    @Test
    void fallsBackToHaversineWhenGHThrows() {
        when(graphHopperService.route(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("routing failed"));

        var bucharest = new Coordinate(44.4268, 26.1025);
        var cluj = new Coordinate(46.7712, 23.6236);
        MatrixResponse result = service.compute(List.of(bucharest, cluj));

        assertThat(result.distances()[0][1]).isBetween(315_000.0, 335_000.0);
    }

    private static GHResponse successResponse(double distanceMeters, long timeMs) {
        ResponsePath path = mock(ResponsePath.class);
        when(path.getDistance()).thenReturn(distanceMeters);
        when(path.getTime()).thenReturn(timeMs);
        GHResponse resp = mock(GHResponse.class);
        when(resp.hasErrors()).thenReturn(false);
        when(resp.getBest()).thenReturn(path);
        return resp;
    }
}
