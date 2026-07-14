package com.bento.tsp.controller;

import com.bento.tsp.model.MatrixResponse;
import com.bento.tsp.security.ApiKeyAuthFilter;
import com.bento.tsp.service.DistanceMatrixService;
import com.bento.tsp.service.GraphHopperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatrixController.class)
@TestPropertySource(properties = "app.security.api-keys=test-key")
class MatrixControllerTest {

    static final String VALID_KEY = "test-key";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    DistanceMatrixService distanceMatrixService;

    @MockBean
    GraphHopperService graphHopperService;

    static final String TWO_COORDS = """
            {"coordinates": [
              {"lat": 44.4268, "lon": 26.1025},
              {"lat": 46.7712, "lon": 23.6236}
            ]}
            """;

    // ── /api/matrix ─────────────────────────────────────────────────────────

    @Test
    void matrixReturns200WithResult() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);
        when(distanceMatrixService.compute(any(List.class))).thenReturn(
                new MatrixResponse(
                        new double[][]{{0, 300_000}, {300_000, 0}},
                        new double[][]{{0, 54_000}, {54_000, 0}}));

        mockMvc.perform(post("/api/matrix")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distances[0][1]").value(300_000.0))
                .andExpect(jsonPath("$.distances[0][0]").value(0.0))
                .andExpect(jsonPath("$.durations[0][1]").value(54_000.0));
    }

    @Test
    void matrixReturns503WhileStarting() throws Exception {
        when(graphHopperService.isReady()).thenReturn(false);

        mockMvc.perform(post("/api/matrix")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void matrixReturns400ForSingleCoordinate() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);

        mockMvc.perform(post("/api/matrix")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"coordinates": [{"lat": 44.4268, "lon": 26.1025}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void matrixReturns400ForNullCoordinates() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);

        mockMvc.perform(post("/api/matrix")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coordinates\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void matrixReturns400ForEmptyBody() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);

        mockMvc.perform(post("/api/matrix")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, VALID_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void matrixReturns401WhenApiKeyMissing() throws Exception {
        mockMvc.perform(post("/api/matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void matrixReturns401WhenApiKeyInvalid() throws Exception {
        mockMvc.perform(post("/api/matrix")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_COORDS))
                .andExpect(status().isUnauthorized());
    }

    // ── /api/health ──────────────────────────────────────────────────────────

    @Test
    void healthReturns200WhenReady() throws Exception {
        when(graphHopperService.isReady()).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("UP"));
    }

    @Test
    void healthReturns503WhileStarting() throws Exception {
        when(graphHopperService.isReady()).thenReturn(false);
        when(graphHopperService.getInitError()).thenReturn(null);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("STARTING"));
    }

    @Test
    void healthReturns500WhenInitFailed() throws Exception {
        when(graphHopperService.isReady()).thenReturn(false);
        when(graphHopperService.getInitError())
                .thenReturn(new RuntimeException("disk full"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("disk full")));
    }
}
