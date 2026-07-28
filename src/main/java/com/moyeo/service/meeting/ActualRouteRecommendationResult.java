package com.moyeo.service.meeting;

import java.util.List;

public record ActualRouteRecommendationResult(Long meetingId, List<Recommendation> recommendations) {
    public record Recommendation(int rank, String areaCode, String areaName, long averageTravelTimeSeconds, long maxTravelTimeSeconds) {}
}
