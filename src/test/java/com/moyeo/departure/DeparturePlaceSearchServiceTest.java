package com.moyeo.departure;

import com.moyeo.domain.departure.DeparturePlaceSearchExecutionPath;
import com.moyeo.global.error.MoyeoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeparturePlaceSearchServiceTest {

    private MockRestServiceServer server;
    private DeparturePlaceSearchService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new DeparturePlaceSearchService(
                builder.build(),
                new DeparturePlaceSearchProperties(
                        "https://dapi.kakao.com",
                        "test-rest-api-key",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3)
                )
        );
    }

    @Test
    void stationSearchKeepsOnlyPlacesWhoseNameStartsWithTheRequestedStation() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "서울역"))
                .andExpect(queryParam("category_group_code", "SW8"))
                .andExpect(header("Authorization", "KakaoAK test-rest-api-key"))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {"place_name":"서울역 1호선","category_group_code":"SW8","address_name":"서울 중구 봉래동2가 122-21","road_address_name":"서울 중구 한강대로 405","x":"126.972135939851","y":"37.5562281317086"},
                            {"place_name":"시청역 1호선","category_group_code":"SW8","address_name":"서울 중구 정동 5-5","road_address_name":"서울 중구 세종대로 101","x":"126.977170100469","y":"37.5657151970389"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        DeparturePlaceSearchService.DeparturePlaceSearchResult result = service.search(" 서울역 ");

        assertThat(result.keyword()).isEqualTo("서울역");
        assertThat(result.executionPath()).isEqualTo(DeparturePlaceSearchExecutionPath.STATION_CATEGORY);
        assertThat(result.places())
                .extracting(DeparturePlaceSearchService.DeparturePlaceSearchResult.Place::displayName)
                .containsExactly("서울역 1호선");
        assertThat(result.places().getFirst().type()).isEqualTo(DeparturePlaceType.STATION);
        assertThat(result.places().getFirst().latitude()).isEqualByComparingTo("37.5562281317086");
        assertThat(result.places().getFirst().longitude()).isEqualByComparingTo("126.972135939851");
        server.verify();
    }

    @Test
    void stationSearchFallsBackToGeneralKeywordWhenStationNameFilteringLeavesNoResult() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "김천구미역"))
                .andExpect(queryParam("category_group_code", "SW8"))
                .andRespond(withSuccess("""
                        { "documents": [{"place_name":"구미역","category_group_code":"SW8","address_name":"경북 구미시 원평동","road_address_name":"","x":"128.330614","y":"36.128000"}] }
                        """, MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "김천구미역"))
                .andRespond(withSuccess("""
                        { "documents": [{"place_name":"김천구미역","category_group_code":"","address_name":"경북 김천시 남면 옥산리","road_address_name":"경북 김천시 남면 혁신1로 51","x":"128.180000","y":"36.113000"}] }
                        """, MediaType.APPLICATION_JSON));

        DeparturePlaceSearchService.DeparturePlaceSearchResult result = service.search("김천구미역");

        assertThat(result.executionPath())
                .isEqualTo(DeparturePlaceSearchExecutionPath.STATION_CATEGORY_TO_KEYWORD);
        assertThat(result.places())
                .extracting(DeparturePlaceSearchService.DeparturePlaceSearchResult.Place::displayName)
                .containsExactly("김천구미역");
        assertThat(result.places().getFirst().type()).isEqualTo(DeparturePlaceType.PLACE);
        server.verify();
    }

    @Test
    void addressSearchMapsRoadAndJibunAddress() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                .andExpect(decodedQueryParam("query", "서울 강남구 테헤란로 152"))
                .andExpect(queryParam("analyze_type", "similar"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "address_name":"서울 강남구 역삼동 737",
                            "address_type":"ROAD_ADDR",
                            "address":{"address_name":"서울 강남구 역삼동 737"},
                            "road_address":{"address_name":"서울 강남구 테헤란로 152","building_name":"강남파이낸스센터"},
                            "x":"127.036502",
                            "y":"37.500028"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        DeparturePlaceSearchService.DeparturePlaceSearchResult result = service.search("서울 강남구 테헤란로 152");

        assertThat(result.executionPath()).isEqualTo(DeparturePlaceSearchExecutionPath.ADDRESS);
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.type()).isEqualTo(DeparturePlaceType.ADDRESS);
            assertThat(place.displayName()).isEqualTo("강남파이낸스센터");
            assertThat(place.address()).isEqualTo("서울 강남구 테헤란로 152");
            assertThat(place.roadAddress()).isEqualTo("서울 강남구 테헤란로 152");
            assertThat(place.jibunAddress()).isEqualTo("서울 강남구 역삼동 737");
            assertThat(place.latitude()).isEqualByComparingTo("37.500028");
            assertThat(place.longitude()).isEqualByComparingTo("127.036502");
        });
        server.verify();
    }

    @Test
    void jibunAddressSearchUsesAddressApiDirectly() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                .andExpect(decodedQueryParam("query", "서울 용산구 동자동 43-205"))
                .andExpect(queryParam("analyze_type", "similar"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "address_name":"서울 용산구 동자동 43-205",
                            "address_type":"REGION_ADDR",
                            "address":{"address_name":"서울 용산구 동자동 43-205"},
                            "road_address":null,
                            "x":"126.970606917394",
                            "y":"37.5546788388674"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        DeparturePlaceSearchService.DeparturePlaceSearchResult result = service.search("서울 용산구 동자동 43-205");

        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.type()).isEqualTo(DeparturePlaceType.ADDRESS);
            assertThat(place.displayName()).isEqualTo("서울 용산구 동자동 43-205");
            assertThat(place.address()).isEqualTo("서울 용산구 동자동 43-205");
            assertThat(place.roadAddress()).isNull();
            assertThat(place.jibunAddress()).isEqualTo("서울 용산구 동자동 43-205");
            assertThat(place.latitude()).isEqualByComparingTo("37.5546788388674");
            assertThat(place.longitude()).isEqualByComparingTo("126.970606917394");
        });
        server.verify();
    }

    @Test
    void strongJibunFormsUseAddressApi() {
        String[] keywords = {
                "경기 부천시 원미구 중동 123-4번지",
                "경기 부천시 원미구 중동 산12-3번지",
                "서울 중구 을지로1가 1-1",
                "제주 제주시 애월읍 애월리 123"
        };
        for (String keyword : keywords) {
            server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                    .andExpect(decodedQueryParam("query", keyword))
                    .andRespond(withSuccess("""
                            {
                              "documents": [{
                                "address_name":"테스트 지번주소",
                                "address_type":"REGION_ADDR",
                                "address":{"address_name":"테스트 지번주소"},
                                "road_address":null,
                                "x":"127.000000",
                                "y":"37.000000"
                              }]
                            }
                            """, MediaType.APPLICATION_JSON));
        }

        for (String keyword : keywords) {
            assertThat(service.search(keyword).places()).singleElement()
                    .satisfies(place -> assertThat(place.type()).isEqualTo(DeparturePlaceType.ADDRESS));
        }
        server.verify();
    }

    @Test
    void broadOrIncompleteAddressLikeTermsUseKeywordApi() {
        String[] keywords = {
                "서울시",
                "원미구",
                "중동",
                "서울 중구 세종대로 지하 2",
                "제주 제주시 애월읍 123",
                "라면 1",
                "어딘가 1"
        };
        for (String keyword : keywords) {
            server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                    .andExpect(decodedQueryParam("query", keyword))
                    .andRespond(withSuccess("{ \"documents\": [] }", MediaType.APPLICATION_JSON));
        }

        for (String keyword : keywords) {
            assertThat(service.search(keyword).places()).isEmpty();
        }
        server.verify();
    }

    @Test
    void generalPlaceSearchUsesKeywordApiWithoutStationCategory() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "스타벅스 강남점"))
                .andExpect(request -> assertThat(request.getURI().getRawQuery())
                        .doesNotContain("category_group_code="))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "place_name":"스타벅스 강남점",
                            "category_group_code":"CE7",
                            "address_name":"서울 강남구 역삼동 820-8",
                            "road_address_name":"서울 강남구 강남대로 390",
                            "x":"127.028000",
                            "y":"37.497000"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        DeparturePlaceSearchService.DeparturePlaceSearchResult result = service.search("스타벅스 강남점");

        assertThat(result.executionPath()).isEqualTo(DeparturePlaceSearchExecutionPath.KEYWORD);
        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.type()).isEqualTo(DeparturePlaceType.PLACE);
            assertThat(place.displayName()).isEqualTo("스타벅스 강남점");
            assertThat(place.address()).isEqualTo("서울 강남구 강남대로 390");
            assertThat(place.roadAddress()).isEqualTo("서울 강남구 강남대로 390");
            assertThat(place.jibunAddress()).isEqualTo("서울 강남구 역삼동 820-8");
            assertThat(place.latitude()).isEqualByComparingTo("37.497000");
            assertThat(place.longitude()).isEqualByComparingTo("127.028000");
        });
        server.verify();
    }

    @Test
    void addressSearchFallsBackToGeneralKeywordWhenItReturnsNoAddress() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                .andExpect(decodedQueryParam("query", "서울 강남구 테헤란로 99999"))
                .andExpect(queryParam("analyze_type", "similar"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "address_name":"서울 강남구 테헤란로",
                            "address_type":"ROAD",
                            "address":null,
                            "road_address":null,
                            "x":"127.040000",
                            "y":"37.501000"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "서울 강남구 테헤란로 99999"))
                .andRespond(withSuccess("""
                        { "documents": [{"place_name":"테헤란로","category_group_code":"","address_name":"서울 강남구","road_address_name":"","x":"127.040000","y":"37.501000"}] }
                        """, MediaType.APPLICATION_JSON));

        DeparturePlaceSearchService.DeparturePlaceSearchResult result = service.search("서울 강남구 테헤란로 99999");

        assertThat(result.executionPath()).isEqualTo(DeparturePlaceSearchExecutionPath.ADDRESS_TO_KEYWORD);
        assertThat(result.places())
                .extracting(DeparturePlaceSearchService.DeparturePlaceSearchResult.Place::displayName)
                .containsExactly("테헤란로");
        server.verify();
    }

    @Test
    void missingDocumentsIsProviderFailureAndDoesNotFallback() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                .andExpect(decodedQueryParam("query", "서울 강남구 테헤란로 152"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("서울 강남구 테헤란로 152"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    @Test
    void missingAddressTypeIsProviderFailureAndDoesNotFallback() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                .andExpect(decodedQueryParam("query", "서울 강남구 테헤란로 152"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "address_name":"서울 강남구 테헤란로 152",
                            "address":null,
                            "road_address":null,
                            "x":"127.036502",
                            "y":"37.500028"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("서울 강남구 테헤란로 152"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    @Test
    void unknownAddressTypeIsProviderFailureAndDoesNotFallback() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                .andExpect(decodedQueryParam("query", "서울 강남구 테헤란로 152"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "address_name":"서울 강남구 테헤란로 152",
                            "address_type":"UNKNOWN",
                            "address":null,
                            "road_address":null,
                            "x":"127.036502",
                            "y":"37.500028"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("서울 강남구 테헤란로 152"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    @Test
    void missingCoordinatesIsProviderFailureAndDoesNotFallback() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "서울역"))
                .andExpect(queryParam("category_group_code", "SW8"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "place_name":"서울역 1호선",
                            "category_group_code":"SW8",
                            "address_name":"서울 중구 봉래동2가 122-21",
                            "road_address_name":"서울 중구 한강대로 405"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("서울역"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    @Test
    void nullKeywordDocumentIsProviderFailureAndDoesNotFallback() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "서울역"))
                .andExpect(queryParam("category_group_code", "SW8"))
                .andRespond(withSuccess("{ \"documents\": [null] }", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("서울역"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    @Test
    void missingKeywordCandidateNameOrAddressIsProviderFailure() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "장소"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "place_name":"",
                            "category_group_code":"",
                            "address_name":"",
                            "road_address_name":"",
                            "x":"127.000000",
                            "y":"37.000000"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("장소"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    @Test
    void missingCompleteAddressTextIsProviderFailureAndDoesNotFallback() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/address.json"))
                .andExpect(decodedQueryParam("query", "서울 강남구 테헤란로 152"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "address_name":"",
                            "address_type":"ROAD_ADDR",
                            "address":null,
                            "road_address":null,
                            "x":"127.036502",
                            "y":"37.500028"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.search("서울 강남구 테헤란로 152"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    @Test
    void providerHttpFailureDoesNotFallback() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json"))
                .andExpect(decodedQueryParam("query", "서울역"))
                .andExpect(queryParam("category_group_code", "SW8"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.search("서울역"))
                .isInstanceOf(MoyeoException.class);
        server.verify();
    }

    private RequestMatcher decodedQueryParam(String name, String expectedValue) {
        return request -> {
            String decodedQuery = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
            assertThat(decodedQuery).contains(name + "=" + expectedValue);
        };
    }
}
