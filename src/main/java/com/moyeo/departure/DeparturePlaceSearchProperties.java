package com.moyeo.departure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "moyeo.departure-place-search")
public record DeparturePlaceSearchProperties(
        String baseUrl,
        String restApiKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
