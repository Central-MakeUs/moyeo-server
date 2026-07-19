package com.moyeo.service.departure;

import com.moyeo.departure.DeparturePlaceSearchService.DeparturePlaceSearchResult;
import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.domain.departure.DeparturePlaceSearch;
import com.moyeo.domain.departure.DeparturePlaceSearchExecutionPath;
import com.moyeo.domain.departure.DeparturePlaceSearchProvider;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlaceRecommendationStrategy;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.domain.member.User;
import com.moyeo.repository.departure.DeparturePlaceSearchRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.member.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class DeparturePlaceSearchHistoryRecorderTest {

    @Autowired
    private DeparturePlaceSearchHistoryRecorder historyRecorder;

    @Autowired
    private DeparturePlaceSearchRepository searchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void memberSearchPersistsNormalizedExecutionAndOrderedCandidates() {
        User user = userRepository.save(new User("검색회원"));
        DeparturePlaceSearchResult result = resultWithCandidate();

        historyRecorder.recordMemberSearch(user.getId(), result);
        entityManager.clear();

        DeparturePlaceSearch saved = searchRepository.findAll().getFirst();
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getMeeting()).isNull();
        assertThat(saved.getKeyword()).isEqualTo("서울역");
        assertThat(saved.getProvider()).isEqualTo(DeparturePlaceSearchProvider.KAKAO_LOCAL);
        assertThat(saved.getExecutionPath()).isEqualTo(DeparturePlaceSearchExecutionPath.STATION_CATEGORY);
        assertThat(saved.getCandidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.getPosition()).isEqualTo(1);
            assertThat(candidate.getType()).isEqualTo(DeparturePlaceType.STATION);
            assertThat(candidate.getDisplayName()).isEqualTo("서울역 1호선");
            assertThat(candidate.getLatitude()).isEqualByComparingTo("37.5562281317086");
            assertThat(candidate.getLongitude()).isEqualByComparingTo("126.972135939851");
        });
    }

    @Test
    void successfulEmptyGuestSearchPersistsOnlyExecutionAgainstMeeting() {
        User host = userRepository.save(new User("검색방장"));
        Meeting meeting = meetingRepository.save(new Meeting(
                host,
                "검색모임",
                null,
                4,
                PlanningType.PLACE_ONLY,
                ScheduleMode.NONE,
                ScheduleInputType.NONE,
                null,
                null,
                null,
                PlaceMode.RECOMMEND,
                PlaceRecommendationStrategy.MIDDLE_POINT,
                null,
                null,
                LocalDateTime.now().plusDays(1),
                "SEARCHTEST"
        ));
        DeparturePlaceSearchResult result = new DeparturePlaceSearchResult(
                "없는 장소",
                DeparturePlaceSearchExecutionPath.KEYWORD,
                List.of()
        );

        historyRecorder.recordGuestSearch(meeting.getId(), result);
        entityManager.clear();

        DeparturePlaceSearch saved = searchRepository.findAll().getFirst();
        assertThat(saved.getUser()).isNull();
        assertThat(saved.getMeeting().getId()).isEqualTo(meeting.getId());
        assertThat(saved.getCandidates()).isEmpty();
    }

    private DeparturePlaceSearchResult resultWithCandidate() {
        return new DeparturePlaceSearchResult(
                "서울역",
                DeparturePlaceSearchExecutionPath.STATION_CATEGORY,
                List.of(new DeparturePlaceSearchResult.Place(
                        DeparturePlaceType.STATION,
                        "서울역 1호선",
                        "서울 중구 한강대로 405",
                        "서울 중구 한강대로 405",
                        "서울 중구 봉래동2가 122-21",
                        new BigDecimal("37.5562281317086"),
                        new BigDecimal("126.972135939851")
                ))
        );
    }
}
