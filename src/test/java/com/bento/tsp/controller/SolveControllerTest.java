package com.bento.tsp.controller;

import com.bento.tsp.model.GeoJsonLineString;
import com.bento.tsp.model.MatrixResponse;
import com.bento.tsp.security.ApiKeyAuthFilter;
import com.bento.tsp.service.DistanceMatrixService;
import com.bento.tsp.service.GraphHopperService;
import com.bento.tsp.service.RouteGeometryService;
import com.bento.tsp.service.TspSolverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolveController.class)
@TestPropertySource(properties = "app.security.api-keys=test-key")
class SolveControllerTest {

    static final String VALID_KEY = "test-key";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    DistanceMatrixService distanceMatrixService;

    @MockBean
    TspSolverService tspSolverService;

    @MockBean
    RouteGeometryService routeGeometryService;

    @MockBean
    GraphHopperService graphHopperService;

    static final String TWO_COORDS_START_0 = """
            {
              "coordinates": [
                {"lat": 44.4268, "lon": 26.1025},
                {"lat": 46.7712, "lon": 23.6236}
              ],
              "startIndex": 0
            }
            """;

    @Test
    void solveReturns200WithOrderedRouteAndGeometry() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);
        when(distanceMatrixService.compute(any())).thenReturn(
                new MatrixResponse(
                        new double[][]{{0, 300_000}, {300_000, 0}},
                        new double[][]{{0, 54_000}, {54_000, 0}}));
        when(tspSolverService.solve(any(), anyInt())).thenReturn(new int[]{0, 1});
        when(routeGeometryService.buildGeometry(any(), any())).thenReturn(
                GeoJsonLineString.of(List.of(
                        new double[]{26.1025, 44.4268},
                        new double[]{23.6236, 46.7712},
                        new double[]{26.1025, 44.4268})));

        mockMvc.perform(post("/api/solve")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS_START_0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedCoordinates[0].lat").value(44.4268))
                .andExpect(jsonPath("$.orderedCoordinates[1].lat").value(46.7712))
                .andExpect(jsonPath("$.geometry.type").value("LineString"))
                .andExpect(jsonPath("$.geometry.coordinates[0][0]").value(26.1025));
    }

    @Test
    void solveReturns503WhileGraphHopperStarting() throws Exception {
        when(graphHopperService.isReady()).thenReturn(false);

        mockMvc.perform(post("/api/solve")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS_START_0))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void solveReturns400WhenStartIndexOutOfBounds() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);

        mockMvc.perform(post("/api/solve")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coordinates": [
                                    {"lat": 44.4268, "lon": 26.1025},
                                    {"lat": 46.7712, "lon": 23.6236}
                                  ],
                                  "startIndex": 5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solveReturns400ForMissingStartIndex() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);

        mockMvc.perform(post("/api/solve")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coordinates": [
                                    {"lat": 44.4268, "lon": 26.1025},
                                    {"lat": 46.7712, "lon": 23.6236}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solveReturns400ForSingleCoordinate() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);

        mockMvc.perform(post("/api/solve")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coordinates": [{"lat": 44.4268, "lon": 26.1025}],
                                  "startIndex": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solveReturns401WhenApiKeyMissing() throws Exception {
        mockMvc.perform(post("/api/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS_START_0))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solveReturns401WhenApiKeyInvalid() throws Exception {
        mockMvc.perform(post("/api/solve")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS_START_0))
                .andExpect(status().isUnauthorized());
    }
}
