package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.TransportationMode;

import java.math.BigDecimal;
import java.util.List;

final class CommercialAreaPreliminaryScoreCalculator {

    private CommercialAreaPreliminaryScoreCalculator() {
    }

    static Score calculate(CommercialArea area, List<ParticipantDeparture> participants) {
        List<Long> distances = participants.stream()
                .map(participant -> Math.round(distanceMeters(
                        participant.latitude().doubleValue(),
                        participant.longitude().doubleValue(),
                        area.latitude().doubleValue(),
                        area.longitude().doubleValue()
                )))
                .toList();
        List<Long> weightedDistances = java.util.stream.IntStream.range(0, participants.size())
                .mapToObj(index -> Math.round(distances.get(index) * transportationWeight(participants.get(index))))
                .toList();

        long averageStraightDistanceMeters = Math.round(
                distances.stream().mapToLong(Long::longValue).average().orElse(0)
        );
        long averageWeightedDistanceMeters = Math.round(
                weightedDistances.stream().mapToLong(Long::longValue).average().orElse(0)
        );
        long maxWeightedDistanceMeters = weightedDistances.stream().mapToLong(Long::longValue).max().orElse(0);
        return new Score(averageStraightDistanceMeters, averageWeightedDistanceMeters + maxWeightedDistanceMeters);
    }

    private static double transportationWeight(ParticipantDeparture participant) {
        return participant.transportationMode() == TransportationMode.PUBLIC_TRANSIT ? 0.9 : 1.0;
    }

    private static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double earthRadiusMeters = 6_371_000;
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLon = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
    }

    record ParticipantDeparture(
            BigDecimal latitude,
            BigDecimal longitude,
            TransportationMode transportationMode
    ) {
    }

    record Score(long averageStraightDistanceMeters, long score) {
    }
}
