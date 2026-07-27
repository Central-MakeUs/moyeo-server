package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.TransportationMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommercialAreaPreliminaryScoreCalculatorTest {

    private static final CommercialArea AREA = new CommercialArea(
            "area", "candidate", "category", BigDecimal.ZERO, BigDecimal.ZERO, "district", "dong"
    );

    @Test
    void appliesTransitWeightBeforeAddingAverageAndMaximumDistance() {
        var publicTransitScore = CommercialAreaPreliminaryScoreCalculator.calculate(
                AREA,
                List.of(
                        departure("1", TransportationMode.PUBLIC_TRANSIT),
                        departure("0", TransportationMode.CAR)
                )
        );
        var carScore = CommercialAreaPreliminaryScoreCalculator.calculate(
                AREA,
                List.of(
                        departure("1", TransportationMode.CAR),
                        departure("0", TransportationMode.CAR)
                )
        );

        assertThat(publicTransitScore.averageStraightDistanceMeters()).isEqualTo(55_598);
        assertThat(publicTransitScore.score()).isEqualTo(150_114);
        assertThat(publicTransitScore.score()).isLessThan(carScore.score());
        assertThat(carScore.score()).isEqualTo(166_793);
    }

    private CommercialAreaPreliminaryScoreCalculator.ParticipantDeparture departure(
            String latitude,
            TransportationMode transportationMode
    ) {
        return new CommercialAreaPreliminaryScoreCalculator.ParticipantDeparture(
                new BigDecimal(latitude), BigDecimal.ZERO, transportationMode
        );
    }
}
