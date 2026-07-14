package com.bento.tsp.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ApiKeyAuthFilterTest {

    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter("key-a, key-b");

    @Test
    void rejectsRequestWithoutApiKeyHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/solve");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void rejectsRequestWithUnknownApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/solve");
        request.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "not-accepted");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void allowsRequestWithAcceptedApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/solve");
        request.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "key-b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsHealthCheckWithoutApiKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
