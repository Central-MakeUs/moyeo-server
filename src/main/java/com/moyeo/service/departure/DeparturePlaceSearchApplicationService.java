package com.moyeo.service.departure;

import com.moyeo.departure.DeparturePlaceSearchService.DeparturePlaceSearchResult;
import com.moyeo.departure.DeparturePlaceSearchService.ReverseGeocodingResult;
import org.springframework.stereotype.Service;

@Service
public class DeparturePlaceSearchApplicationService {

    private final com.moyeo.departure.DeparturePlaceSearchService providerSearchService;

    public DeparturePlaceSearchApplicationService(com.moyeo.departure.DeparturePlaceSearchService providerSearchService) {
        this.providerSearchService = providerSearchService;
    }

    public DeparturePlaceSearchResult search(String keyword) {
        return providerSearchService.search(keyword);
    }

    public ReverseGeocodingResult reverseGeocode(java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        return providerSearchService.reverseGeocode(latitude, longitude);
    }
}
