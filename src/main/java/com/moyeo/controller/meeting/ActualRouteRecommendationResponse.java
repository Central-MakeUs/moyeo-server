package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ActualRouteRecommendationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "실제 이동시간 기반 장소 추천 응답")
public record ActualRouteRecommendationResponse(Long meetingId, List<Recommendation> recommendations) {
    public static ActualRouteRecommendationResponse from(ActualRouteRecommendationResult result) {
        return new ActualRouteRecommendationResponse(result.meetingId(), result.recommendations().stream().map(r -> new Recommendation(r.rank(), r.areaCode(), r.areaName(), r.averageTravelTimeSeconds(), r.maxTravelTimeSeconds())).toList());
    }
    public record Recommendation(int rank, String areaCode, String areaName, long averageTravelTimeSeconds, long maxTravelTimeSeconds) {}
}
