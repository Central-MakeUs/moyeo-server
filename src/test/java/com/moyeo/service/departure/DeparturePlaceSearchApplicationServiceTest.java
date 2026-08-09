package com.moyeo.service.departure;

import com.moyeo.departure.DeparturePlaceSearchService.DeparturePlaceSearchResult;
import com.moyeo.domain.departure.DeparturePlaceSearchExecutionPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeparturePlaceSearchApplicationServiceTest {

    private final com.moyeo.departure.DeparturePlaceSearchService providerSearchService =
            mock(com.moyeo.departure.DeparturePlaceSearchService.class);
    private final DeparturePlaceSearchApplicationService service =
            new DeparturePlaceSearchApplicationService(providerSearchService);

    @Test
    void returnsProviderSearchResultWithoutPersistingHistory() {
        DeparturePlaceSearchResult result = successfulEmptyResult();
        when(providerSearchService.search("서울역")).thenReturn(result);

        DeparturePlaceSearchResult actual = service.search("서울역");

        assertThat(actual).isSameAs(result);
    }

    @Test
    void providerFailureIsNotRecorded() {
        RuntimeException providerFailure = new RuntimeException("provider failure");
        when(providerSearchService.search("서울역")).thenThrow(providerFailure);

        assertThatThrownBy(() -> service.search("서울역"))
                .isSameAs(providerFailure);
    }

    private DeparturePlaceSearchResult successfulEmptyResult() {
        return new DeparturePlaceSearchResult(
                "서울역",
                DeparturePlaceSearchExecutionPath.STATION_CATEGORY_TO_KEYWORD,
                List.of()
        );
    }
}
