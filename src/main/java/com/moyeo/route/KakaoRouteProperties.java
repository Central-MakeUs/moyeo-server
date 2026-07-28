package com.moyeo.route;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "moyeo.actual-route")
public record KakaoRouteProperties(String mapBaseUrl, String naviBaseUrl, String restApiKey,
        int preliminaryCandidateCount, int finalRecommendationCount, Duration cooldown,
        Duration connectTimeout, Duration readTimeout) {
}
