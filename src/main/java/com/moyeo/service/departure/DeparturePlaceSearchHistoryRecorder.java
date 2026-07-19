package com.moyeo.service.departure;

import com.moyeo.departure.DeparturePlaceSearchService.DeparturePlaceSearchResult;
import com.moyeo.domain.departure.DeparturePlaceSearch;
import com.moyeo.domain.departure.DeparturePlaceSearchProvider;
import com.moyeo.repository.departure.DeparturePlaceSearchRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.member.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeparturePlaceSearchHistoryRecorder {

    private final DeparturePlaceSearchRepository searchRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    public DeparturePlaceSearchHistoryRecorder(
            DeparturePlaceSearchRepository searchRepository,
            UserRepository userRepository,
            MeetingRepository meetingRepository
    ) {
        this.searchRepository = searchRepository;
        this.userRepository = userRepository;
        this.meetingRepository = meetingRepository;
    }

    @Transactional
    public void recordMemberSearch(Long userId, DeparturePlaceSearchResult result) {
        DeparturePlaceSearch search = DeparturePlaceSearch.member(
                userRepository.getReferenceById(userId),
                result.keyword(),
                DeparturePlaceSearchProvider.KAKAO_LOCAL,
                result.executionPath()
        );
        save(search, result);
    }

    @Transactional
    public void recordGuestSearch(Long meetingId, DeparturePlaceSearchResult result) {
        DeparturePlaceSearch search = DeparturePlaceSearch.guest(
                meetingRepository.getReferenceById(meetingId),
                result.keyword(),
                DeparturePlaceSearchProvider.KAKAO_LOCAL,
                result.executionPath()
        );
        save(search, result);
    }

    private void save(DeparturePlaceSearch search, DeparturePlaceSearchResult result) {
        for (int index = 0; index < result.places().size(); index++) {
            DeparturePlaceSearchResult.Place place = result.places().get(index);
            search.addCandidate(
                    index + 1,
                    place.type(),
                    place.displayName(),
                    place.address(),
                    place.roadAddress(),
                    place.jibunAddress(),
                    place.latitude(),
                    place.longitude()
            );
        }
        searchRepository.saveAndFlush(search);
    }
}
