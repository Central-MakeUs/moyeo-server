package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.route.KakaoRouteClient;
import com.moyeo.route.KakaoRouteProperties;
import com.moyeo.route.KakaoRouteUnavailableException;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActualRouteRecommendationService {
    private final MeetingRepository meetings; private final MeetingParticipantRepository participants;
    private final MeetingService meetingService; private final KakaoRouteClient routes; private final KakaoRouteProperties properties;
    private final Map<Long, Instant> lastRequests = new ConcurrentHashMap<>();
    private final Map<Long, Object> calculationLocks = new ConcurrentHashMap<>();
    public ActualRouteRecommendationService(MeetingRepository meetings, MeetingParticipantRepository participants, MeetingService meetingService, KakaoRouteClient routes, KakaoRouteProperties properties) {
        this.meetings=meetings; this.participants=participants; this.meetingService=meetingService; this.routes=routes; this.properties=properties;
    }
    public ActualRouteRecommendationResult calculate(String inviteCode, AuthenticatedMember member) {
        Meeting initialMeeting=meetings.findByInviteCode(inviteCode).orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
        synchronized (calculationLocks.computeIfAbsent(initialMeeting.getId(), ignored -> new Object())) {
            return calculateLocked(inviteCode, member);
        }
    }
    private ActualRouteRecommendationResult calculateLocked(String inviteCode, AuthenticatedMember member) {
        Meeting meeting=meetings.findByInviteCode(inviteCode).orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
        if (!meeting.getHostUser().getId().equals(member.userId())) throw new MoyeoException(MeetingErrorCode.ACTUAL_ROUTE_RECOMMENDATION_FORBIDDEN);
        Instant previous=lastRequests.get(meeting.getId()); if(previous!=null && previous.plus(properties.cooldown()).isAfter(Instant.now())) throw new MoyeoException(MeetingErrorCode.ACTUAL_ROUTE_RECOMMENDATION_COOLDOWN);
        List<MeetingParticipant> all=participants.findAllByMeetingIdOrderByIdAsc(meeting.getId());
        if(all.stream().anyMatch(p -> p.getDepartureLatitude()==null || p.getDepartureLongitude()==null || p.getTransportationMode()==null)) throw new MoyeoException(MeetingErrorCode.ACTUAL_ROUTE_RECOMMENDATION_NOT_READY);
        List<PlaceViewResult.Recommendation> candidates=meetingService.getPlaceView(inviteCode).recommendations().stream().limit(properties.preliminaryCandidateCount()).toList();
        List<ActualRouteRecommendationResult.Recommendation> result;
        try { result=candidates.stream().map(c -> score(c, all)).sorted(actualTimeComparator()).limit(properties.finalRecommendationCount()).toList(); }
        catch (KakaoRouteUnavailableException exception) { throw new MoyeoException(MeetingErrorCode.ACTUAL_ROUTE_RECOMMENDATION_UNAVAILABLE); }
        lastRequests.put(meeting.getId(), Instant.now());
        List<ActualRouteRecommendationResult.Recommendation> ranked=new ArrayList<>(); for(int i=0;i<result.size();i++){var r=result.get(i); ranked.add(new ActualRouteRecommendationResult.Recommendation(i+1,r.areaCode(),r.areaName(),r.averageTravelTimeSeconds(),r.maxTravelTimeSeconds()));}
        return new ActualRouteRecommendationResult(meeting.getId(), ranked);
    }
    private ActualRouteRecommendationResult.Recommendation score(PlaceViewResult.Recommendation c,List<MeetingParticipant> all){LongSummaryStatistics s=all.stream().mapToLong(p->routes.findShortestTravelTimeSeconds(p.getTransportationMode(),p.getDepartureLatitude(),p.getDepartureLongitude(),c.latitude(),c.longitude())).summaryStatistics(); return new ActualRouteRecommendationResult.Recommendation(0,c.areaCode(),c.areaName(),Math.round(s.getAverage()),s.getMax());}
    static Comparator<ActualRouteRecommendationResult.Recommendation> actualTimeComparator() {
        return Comparator.comparingLong(ActualRouteRecommendationService::actualTimeScore)
                .thenComparingLong(ActualRouteRecommendationResult.Recommendation::averageTravelTimeSeconds)
                .thenComparingLong(ActualRouteRecommendationResult.Recommendation::maxTravelTimeSeconds);
    }
    private static long actualTimeScore(ActualRouteRecommendationResult.Recommendation recommendation) { return recommendation.averageTravelTimeSeconds() + recommendation.maxTravelTimeSeconds(); }
}
