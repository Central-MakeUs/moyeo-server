package com.moyeo.departure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moyeo.domain.departure.DeparturePlaceSearchExecutionPath;
import com.moyeo.domain.departure.DepartureRegionPolicy;
import com.moyeo.global.error.MoyeoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DeparturePlaceSearchService {

    private static final Logger log = LoggerFactory.getLogger(DeparturePlaceSearchService.class);
    private static final String SUBWAY_STATION_CATEGORY = "SW8";
    private static final String REGION_ADDRESS_TYPE = "REGION_ADDR";
    private static final String ROAD_ADDRESS_TYPE = "ROAD_ADDR";
    private static final int SEARCH_SIZE = 15;
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final Pattern ROAD_ADDRESS_PATTERN = Pattern.compile(".*(?:대로|로|길)\\s*\\d+(?:-\\d+)?(?:\\s|$).*");
    private static final Pattern JIBUN_ADDRESS_PATTERN = Pattern.compile(
            ".*(?:동|리|\\d+가)\\s*(?:산\\s*)?\\d+(?:-\\d+)?(?:번지)?(?:\\s|$).*"
    );

    private final RestClient restClient;
    private final DeparturePlaceSearchProperties properties;

    public DeparturePlaceSearchService(
            @Qualifier("departurePlaceRestClient") RestClient restClient,
            DeparturePlaceSearchProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public DeparturePlaceSearchResult search(String keyword) {
        String normalizedKeyword = normalize(keyword);
        requireApiKey();

        if (isExactStationQuery(normalizedKeyword)) {
            List<DeparturePlaceSearchResult.Place> stations = searchKeyword(normalizedKeyword, SUBWAY_STATION_CATEGORY)
                    .stream()
                    .filter(place -> hasStationNamePrefix(place.displayName(), normalizedKeyword))
                    .toList();
            if (stations.isEmpty()) {
                return new DeparturePlaceSearchResult(normalizedKeyword,
                        DeparturePlaceSearchExecutionPath.STATION_CATEGORY_TO_KEYWORD,
                        filterSupportedDepartureRegions(searchKeyword(normalizedKeyword, null)));
            }
            return new DeparturePlaceSearchResult(normalizedKeyword,
                    DeparturePlaceSearchExecutionPath.STATION_CATEGORY, filterSupportedDepartureRegions(stations));
        }

        if (isAddressLike(normalizedKeyword)) {
            List<DeparturePlaceSearchResult.Place> addresses = searchAddress(normalizedKeyword);
            if (addresses.isEmpty()) {
                return new DeparturePlaceSearchResult(normalizedKeyword,
                        DeparturePlaceSearchExecutionPath.ADDRESS_TO_KEYWORD,
                        filterSupportedDepartureRegions(searchKeyword(normalizedKeyword, null)));
            }
            return new DeparturePlaceSearchResult(normalizedKeyword,
                    DeparturePlaceSearchExecutionPath.ADDRESS, filterSupportedDepartureRegions(addresses));
        }

        return new DeparturePlaceSearchResult(normalizedKeyword,
                DeparturePlaceSearchExecutionPath.KEYWORD,
                filterSupportedDepartureRegions(searchKeyword(normalizedKeyword, null)));
    }

    public ReverseGeocodingResult reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        requireApiKey(DeparturePlaceSearchErrorCode.REVERSE_GEOCODING_UNAVAILABLE);
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/v2/local/geo/coord2address.json")
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .queryParam("input_coord", "WGS84")
                .build()
                .encode()
                .toUri();
        try {
            KakaoReverseGeocodingResponse response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey().strip())
                    .retrieve()
                    .body(KakaoReverseGeocodingResponse.class);
            if (response == null || response.documents() == null) {
                throw reverseGeocodingUnavailable();
            }
            if (response.documents().isEmpty()) {
                return new ReverseGeocodingResult(null, null, false);
            }
            KakaoReverseGeocodingDocument document = response.documents().getFirst();
            if (document == null) {
                throw reverseGeocodingUnavailable();
            }
            String roadAddress = document.roadAddress() != null
                    ? blankToNull(document.roadAddress().addressName()) : null;
            String jibunAddress = document.address() != null
                    ? blankToNull(document.address().addressName()) : null;
            return new ReverseGeocodingResult(
                    roadAddress,
                    jibunAddress,
                    DepartureRegionPolicy.isSupportedAddress(roadAddress)
                            || DepartureRegionPolicy.isSupportedAddress(jibunAddress)
            );
        } catch (RestClientException exception) {
            log.warn("Kakao reverse geocoding request failed: {}", exception.getClass().getSimpleName());
            throw reverseGeocodingUnavailable();
        }
    }

    private List<DeparturePlaceSearchResult.Place> searchKeyword(String keyword, String categoryGroupCode) {
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/v2/local/search/keyword.json")
                .queryParam("query", keyword)
                .queryParam("size", SEARCH_SIZE)
                .queryParamIfPresent("category_group_code", java.util.Optional.ofNullable(categoryGroupCode))
                .build()
                .encode()
                .toUri();
        try {
            KakaoKeywordResponse response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey().strip())
                    .retrieve()
                    .body(KakaoKeywordResponse.class);
            if (response == null || response.documents() == null) {
                throw unavailable();
            }
            return response.documents().stream()
                    .map(this::mapKeywordDocument)
                    .toList();
        } catch (RestClientException exception) {
            log.warn("Kakao keyword place search request failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private List<DeparturePlaceSearchResult.Place> searchAddress(String keyword) {
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/v2/local/search/address.json")
                .queryParam("query", keyword)
                .queryParam("analyze_type", "similar")
                .queryParam("size", SEARCH_SIZE)
                .build()
                .encode()
                .toUri();
        try {
            KakaoAddressResponse response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey().strip())
                    .retrieve()
                    .body(KakaoAddressResponse.class);
            if (response == null || response.documents() == null) {
                throw unavailable();
            }
            return response.documents().stream()
                    .filter(this::isDetailedAddressDocument)
                    .map(this::mapAddressDocument)
                    .toList();
        } catch (RestClientException exception) {
            log.warn("Kakao address search request failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private void requireApiKey() {
        requireApiKey(DeparturePlaceSearchErrorCode.DEPARTURE_PLACE_SEARCH_UNAVAILABLE);
    }

    private void requireApiKey(DeparturePlaceSearchErrorCode errorCode) {
        if (properties.restApiKey() == null || properties.restApiKey().isBlank()) {
            log.warn("Kakao Local is unavailable because KAKAO_LOCAL_REST_API_KEY is not configured.");
            throw new MoyeoException(errorCode);
        }
    }

    private MoyeoException unavailable() {
        return new MoyeoException(DeparturePlaceSearchErrorCode.DEPARTURE_PLACE_SEARCH_UNAVAILABLE);
    }

    private MoyeoException reverseGeocodingUnavailable() {
        return new MoyeoException(DeparturePlaceSearchErrorCode.REVERSE_GEOCODING_UNAVAILABLE);
    }

    private boolean isExactStationQuery(String keyword) {
        return keyword.length() > 1 && keyword.endsWith("역");
    }

    private boolean isAddressLike(String keyword) {
        return ROAD_ADDRESS_PATTERN.matcher(keyword).matches() || JIBUN_ADDRESS_PATTERN.matcher(keyword).matches();
    }

    private DeparturePlaceSearchResult.Place mapKeywordDocument(KakaoKeywordDocument document) {
        if (document == null) {
            throw unavailable();
        }
        String roadAddress = blankToNull(document.roadAddressName());
        String jibunAddress = blankToNull(document.addressName());
        return new DeparturePlaceSearchResult.Place(
                SUBWAY_STATION_CATEGORY.equals(document.categoryGroupCode())
                        ? DeparturePlaceType.STATION
                        : DeparturePlaceType.PLACE,
                requiredText(document.placeName()),
                requiredText(preferredAddress(roadAddress, jibunAddress)),
                roadAddress,
                jibunAddress,
                latitude(document.y()),
                longitude(document.x())
        );
    }

    private boolean isDetailedAddressDocument(KakaoAddressDocument document) {
        if (document == null) {
            throw unavailable();
        }
        return isDetailedAddressType(document.addressType());
    }

    private DeparturePlaceSearchResult.Place mapAddressDocument(KakaoAddressDocument document) {
        String roadAddress = document.roadAddress() != null
                ? blankToNull(document.roadAddress().addressName()) : null;
        String jibunAddress = document.address() != null
                ? blankToNull(document.address().addressName()) : null;
        String address = requiredText(preferredAddress(roadAddress, firstText(document.addressName(), jibunAddress)));
        String buildingName = document.roadAddress() != null
                ? blankToNull(document.roadAddress().buildingName()) : null;
        return new DeparturePlaceSearchResult.Place(
                DeparturePlaceType.ADDRESS,
                firstText(buildingName, address),
                address,
                roadAddress,
                jibunAddress,
                latitude(document.y()),
                longitude(document.x())
        );
    }

    private boolean isDetailedAddressType(String addressType) {
        String normalizedAddressType = blankToNull(addressType);
        if (normalizedAddressType == null) {
            throw unavailable();
        }
        return switch (normalizedAddressType) {
            case REGION_ADDRESS_TYPE, ROAD_ADDRESS_TYPE -> true;
            case "REGION", "ROAD" -> false;
            default -> throw unavailable();
        };
    }

    private List<DeparturePlaceSearchResult.Place> filterSupportedDepartureRegions(
            List<DeparturePlaceSearchResult.Place> places
    ) {
        return places.stream()
                .filter(place -> DepartureRegionPolicy.isSupportedAddress(place.address()))
                .toList();
    }

    private boolean hasStationNamePrefix(String placeName, String stationName) {
        if (placeName == null) {
            return false;
        }
        String normalizedPlaceName = normalize(placeName);
        if (!normalizedPlaceName.startsWith(stationName)) {
            return false;
        }
        if (normalizedPlaceName.length() == stationName.length()) {
            return true;
        }
        char followingCharacter = normalizedPlaceName.charAt(stationName.length());
        return Character.isWhitespace(followingCharacter)
                || followingCharacter == '('
                || followingCharacter == '（';
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }

    private String preferredAddress(String roadAddress, String jibunAddress) {
        return firstText(roadAddress, jibunAddress);
    }

    private String firstText(String... candidates) {
        for (String candidate : candidates) {
            String normalizedCandidate = blankToNull(candidate);
            if (normalizedCandidate != null) {
                return normalizedCandidate;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String requiredText(String value) {
        String normalizedValue = blankToNull(value);
        if (normalizedValue == null) {
            throw unavailable();
        }
        return normalizedValue;
    }

    private BigDecimal latitude(String value) {
        return coordinate(value, MIN_LATITUDE, MAX_LATITUDE);
    }

    private BigDecimal longitude(String value) {
        return coordinate(value, MIN_LONGITUDE, MAX_LONGITUDE);
    }

    private BigDecimal coordinate(String value, BigDecimal minimum, BigDecimal maximum) {
        String normalizedValue = blankToNull(value);
        if (normalizedValue == null) {
            throw unavailable();
        }
        try {
            BigDecimal coordinate = new BigDecimal(normalizedValue);
            if (coordinate.compareTo(minimum) < 0 || coordinate.compareTo(maximum) > 0) {
                throw unavailable();
            }
            return coordinate;
        } catch (NumberFormatException exception) {
            throw unavailable();
        }
    }

    public record DeparturePlaceSearchResult(
            String keyword,
            DeparturePlaceSearchExecutionPath executionPath,
            List<Place> places
    ) {
        public record Place(
                DeparturePlaceType type,
                String displayName,
                String address,
                String roadAddress,
                String jibunAddress,
                BigDecimal latitude,
                BigDecimal longitude
        ) {
        }
    }

    public record ReverseGeocodingResult(String roadAddress, String jibunAddress, boolean isSupportedRegion) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoKeywordResponse(List<KakaoKeywordDocument> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoKeywordDocument(
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_group_code") String categoryGroupCode,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            String x,
            String y
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAddressResponse(List<KakaoAddressDocument> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAddressDocument(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("address_type") String addressType,
            KakaoJibunAddress address,
            @JsonProperty("road_address") KakaoRoadAddress roadAddress,
            String x,
            String y
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoJibunAddress(@JsonProperty("address_name") String addressName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoRoadAddress(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("building_name") String buildingName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoReverseGeocodingResponse(List<KakaoReverseGeocodingDocument> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoReverseGeocodingDocument(
            KakaoJibunAddress address,
            @JsonProperty("road_address") KakaoRoadAddress roadAddress
    ) {
    }
}
