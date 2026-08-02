package com.moyeo.route;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "moyeo.actual-route")
public record KakaoRouteProperties(String mapBaseUrl, String naviBaseUrl, String restApiKey,
        @Min(1) @Max(3) int preliminaryCandidateCount, int finalRecommendationCount,
        java.time.Duration connectTimeout, java.time.Duration readTimeout) {
}
