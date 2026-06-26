package com.bento.tsp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.graphhopper")
public record GraphHopperProperties(
        String graphLocation,
        String osmFile,
        String osmDownloadUrl,
        int threadPoolSize,
        String profile,
        double haversineFallbackSpeedKmh
) {}
