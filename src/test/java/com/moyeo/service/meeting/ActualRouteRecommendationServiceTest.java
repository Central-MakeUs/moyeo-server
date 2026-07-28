package com.moyeo.service.meeting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActualRouteRecommendationServiceTest {

    @Test
    void ranksByTravelTimeAveragePlusMaximumNotAverageAlone() {
        var lowerAverageButHigherTotal = new ActualRouteRecommendationResult.Recommendation(0, "a", "a", 100, 500);
        var higherAverageButLowerTotal = new ActualRouteRecommendationResult.Recommendation(0, "b", "b", 200, 250);

        var ranked = List.of(lowerAverageButHigherTotal, higherAverageButLowerTotal).stream()
                .sorted(ActualRouteRecommendationService.actualTimeComparator())
                .toList();

        assertThat(ranked).containsExactly(higherAverageButLowerTotal, lowerAverageButHigherTotal);
    }
}
