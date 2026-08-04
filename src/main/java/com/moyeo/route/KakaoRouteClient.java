package com.moyeo.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.moyeo.domain.meeting.TransportationMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@Component
public class KakaoRouteClient {

    private final RestClient restClient;
    private final KakaoRouteProperties properties;

    public KakaoRouteClient(
            @Qualifier("kakaoRouteRestClient") RestClient restClient,
            KakaoRouteProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public long findShortestTravelTimeSeconds(
            TransportationMode mode,
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            BigDecimal destinationLatitude,
            BigDecimal destinationLongitude
    ) {
        requireApiKey();
        try {
            return mode == TransportationMode.CAR
                    ? drivingDuration(originLatitude, originLongitude, destinationLatitude, destinationLongitude)
                    : publicTransitDuration(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        } catch (RestClientException exception) {
            throw new KakaoRouteUnavailableException(exception);
        }
    }

    private long publicTransitDuration(BigDecimal originLatitude, BigDecimal originLongitude,
                                       BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        URI uri = UriComponentsBuilder.fromUriString(properties.mapBaseUrl())
                .path("/v2/routing/publictraffic")
                .queryParam("start_x", originLongitude).queryParam("start_y", originLatitude)
                .queryParam("end_x", destinationLongitude).queryParam("end_y", destinationLatitude)
                .build().encode().toUri();
        JsonNode response = get(uri);
        if ("NO_RESULTS".equals(response.path("status").asText())) {
            return walkingDuration(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        }
        return minimum(response.path("routes"), "properties", "totalTime");
    }

    private long walkingDuration(BigDecimal originLatitude, BigDecimal originLongitude,
                                 BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        URI uri = UriComponentsBuilder.fromUriString(properties.mapBaseUrl())
                .path("/v2/routing/walk")
                .queryParam("start_x", originLongitude).queryParam("start_y", originLatitude)
                .queryParam("end_x", destinationLongitude).queryParam("end_y", destinationLatitude)
                .build().encode().toUri();
        JsonNode response = get(uri);
        JsonNode totalTime = response.path("route").path("properties").path("totalTime");
        if (totalTime.canConvertToLong() && totalTime.longValue() >= 0) return totalTime.longValue();
        throw new KakaoRouteUnavailableException(null);
    }

    private long drivingDuration(BigDecimal originLatitude, BigDecimal originLongitude,
                                 BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        URI uri = UriComponentsBuilder.fromUriString(properties.naviBaseUrl())
                .path("/v1/directions")
                .queryParam("origin", originLongitude + "," + originLatitude)
                .queryParam("destination", destinationLongitude + "," + destinationLatitude)
                .build().encode().toUri();
        JsonNode response = get(uri);
        return minimum(response.path("routes"), "summary", "duration");
    }

    private JsonNode get(URI uri) {
        JsonNode response = restClient.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey().strip())
                .retrieve().body(JsonNode.class);
        if (response == null) throw new KakaoRouteUnavailableException(null);
        return response;
    }

    private long minimum(JsonNode routes, String propertiesName, String timeName) {
        long minimum = Long.MAX_VALUE;
        for (JsonNode route : routes) {
            JsonNode value = route.path(propertiesName).path(timeName);
            if (value.canConvertToLong() && value.longValue() >= 0) minimum = Math.min(minimum, value.longValue());
        }
        if (minimum == Long.MAX_VALUE) throw new KakaoRouteUnavailableException(null);
        return minimum;
    }

    private void requireApiKey() {
        if (properties.restApiKey() == null || properties.restApiKey().isBlank()) throw new KakaoRouteUnavailableException(null);
    }
}
