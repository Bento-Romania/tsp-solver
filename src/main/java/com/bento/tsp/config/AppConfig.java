package com.bento.tsp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(GraphHopperProperties.class)
public class AppConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService routingExecutor(GraphHopperProperties props) {
        return Executors.newFixedThreadPool(props.threadPoolSize());
    }
}
