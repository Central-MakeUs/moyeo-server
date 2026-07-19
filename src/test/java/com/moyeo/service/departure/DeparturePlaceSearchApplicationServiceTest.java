package com.moyeo.service.departure;

import com.moyeo.departure.DeparturePlaceSearchService.DeparturePlaceSearchResult;
import com.moyeo.domain.departure.DeparturePlaceSearchExecutionPath;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeparturePlaceSearchApplicationServiceTest {

    private final com.moyeo.departure.DeparturePlaceSearchService providerSearchService =
            mock(com.moyeo.departure.DeparturePlaceSearchService.class);
    private final DeparturePlaceSearchHistoryRecorder historyRecorder =
            mock(DeparturePlaceSearchHistoryRecorder.class);
    private final DeparturePlaceSearchApplicationService service =
            new DeparturePlaceSearchApplicationService(providerSearchService, historyRecorder);

    @Test
    void successfulMemberSearchIsRecorded() {
        DeparturePlaceSearchResult result = successfulEmptyResult();
        when(providerSearchService.search("서울역")).thenReturn(result);

        DeparturePlaceSearchResult actual = service.searchForMember(1L, "서울역");

        assertThat(actual).isSameAs(result);
        verify(historyRecorder).recordMemberSearch(1L, result);
    }

    @Test
    void successfulGuestSearchIsRecordedAgainstMeeting() {
        DeparturePlaceSearchResult result = successfulEmptyResult();
        when(providerSearchService.search("서울역")).thenReturn(result);

        DeparturePlaceSearchResult actual = service.searchForGuest(2L, "서울역");

        assertThat(actual).isSameAs(result);
        verify(historyRecorder).recordGuestSearch(2L, result);
    }

    @Test
    void providerFailureIsNotRecorded() {
        RuntimeException providerFailure = new RuntimeException("provider failure");
        when(providerSearchService.search("서울역")).thenThrow(providerFailure);

        assertThatThrownBy(() -> service.searchForMember(1L, "서울역"))
                .isSameAs(providerFailure);
        verifyNoInteractions(historyRecorder);
    }

    @Test
    void historyFailureDoesNotFailSuccessfulSearch() {
        DeparturePlaceSearchResult result = successfulEmptyResult();
        when(providerSearchService.search("서울역")).thenReturn(result);
        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(historyRecorder).recordMemberSearch(1L, result);

        assertThat(service.searchForMember(1L, "서울역")).isSameAs(result);
    }

    private DeparturePlaceSearchResult successfulEmptyResult() {
        return new DeparturePlaceSearchResult(
                "서울역",
                DeparturePlaceSearchExecutionPath.STATION_CATEGORY_TO_KEYWORD,
                List.of()
        );
    }
}
