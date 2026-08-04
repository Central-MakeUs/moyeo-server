package com.moyeo.route;

import com.moyeo.domain.meeting.TransportationMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoRouteClientTest {

    private MockRestServiceServer server;
    private KakaoRouteClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoRouteClient(
                builder.build(),
                new KakaoRouteProperties(
                        "https://dapi.kakao.com",
                        "https://apis-navi.kakaomobility.com",
                        "test-rest-api-key",
                        3,
                        3,
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)
                )
        );
    }

    @Test
    void publicTransitNoResultsFallsBackToWalkingDuration() {
        expectRouteRequest("/v2/routing/publictraffic")
                .andRespond(withSuccess("""
                        {"status":"NO_RESULTS","properties":{"total":0},"routes":[]}
                        """, MediaType.APPLICATION_JSON));
        expectRouteRequest("/v2/routing/walk")
                .andRespond(withSuccess("""
                        {"status":"OK","route":{"properties":{"totalTime":284}}}
                        """, MediaType.APPLICATION_JSON));

        long duration = client.findShortestTravelTimeSeconds(
                TransportationMode.PUBLIC_TRANSIT,
                new BigDecimal("37.5202231"), new BigDecimal("126.8528896"),
                new BigDecimal("37.5218464"), new BigDecimal("126.8526352")
        );

        assertThat(duration).isEqualTo(284);
        server.verify();
    }

    @Test
    void walkingNoResultsRemainsUnavailable() {
        expectRouteRequest("/v2/routing/publictraffic")
                .andRespond(withSuccess("""
                        {"status":"NO_RESULTS","properties":{"total":0},"routes":[]}
                        """, MediaType.APPLICATION_JSON));
        expectRouteRequest("/v2/routing/walk")
                .andRespond(withSuccess("""
                        {"status":"NO_RESULTS"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.findShortestTravelTimeSeconds(
                TransportationMode.PUBLIC_TRANSIT,
                new BigDecimal("37.5202231"), new BigDecimal("126.8528896"),
                new BigDecimal("37.5218464"), new BigDecimal("126.8526352")
        )).isInstanceOf(KakaoRouteUnavailableException.class);
        server.verify();
    }

    private ResponseActions expectRouteRequest(String path) {
        return server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo(path))
                .andExpect(queryParam("start_x", "126.8528896"))
                .andExpect(queryParam("start_y", "37.5202231"))
                .andExpect(queryParam("end_x", "126.8526352"))
                .andExpect(queryParam("end_y", "37.5218464"))
                .andExpect(header("Authorization", "KakaoAK test-rest-api-key"));
    }
}
