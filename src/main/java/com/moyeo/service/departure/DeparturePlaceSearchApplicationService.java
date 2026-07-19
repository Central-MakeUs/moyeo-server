package com.moyeo.service.departure;

import com.moyeo.departure.DeparturePlaceSearchService.DeparturePlaceSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class DeparturePlaceSearchApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeparturePlaceSearchApplicationService.class);

    private final com.moyeo.departure.DeparturePlaceSearchService providerSearchService;
    private final DeparturePlaceSearchHistoryRecorder historyRecorder;

    public DeparturePlaceSearchApplicationService(
            com.moyeo.departure.DeparturePlaceSearchService providerSearchService,
            DeparturePlaceSearchHistoryRecorder historyRecorder
    ) {
        this.providerSearchService = providerSearchService;
        this.historyRecorder = historyRecorder;
    }

    public DeparturePlaceSearchResult searchForMember(Long userId, String keyword) {
        DeparturePlaceSearchResult result = providerSearchService.search(keyword);
        try {
            historyRecorder.recordMemberSearch(userId, result);
        } catch (DataAccessException exception) {
            log.error("Failed to persist member departure place search history: {}",
                    exception.getClass().getSimpleName());
        }
        return result;
    }

    public DeparturePlaceSearchResult searchForGuest(Long meetingId, String keyword) {
        DeparturePlaceSearchResult result = providerSearchService.search(keyword);
        try {
            historyRecorder.recordGuestSearch(meetingId, result);
        } catch (DataAccessException exception) {
            log.error("Failed to persist guest departure place search history: {}",
                    exception.getClass().getSimpleName());
        }
        return result;
    }
}
