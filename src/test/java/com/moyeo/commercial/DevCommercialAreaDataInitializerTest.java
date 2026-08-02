package com.moyeo.commercial;

import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.repository.commercial.CommercialAreaRepository;
import com.moyeo.repository.commercial.CommercialAreaStationLineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:commercial-area-dev-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "moyeo.jwt.secret=commercial-area-dev-test-jwt-secret-commercial-area-dev-test"
})
@ActiveProfiles("dev")
class DevCommercialAreaDataInitializerTest {

    @Autowired
    private CommercialAreaRepository commercialAreaRepository;

    @Autowired
    private CommercialAreaStationLineRepository commercialAreaStationLineRepository;

    @Test
    void devProfileLoadsTheConfirmedSeoulCommercialAreaSeed() {
        assertThat(commercialAreaRepository.countBySource(CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS))
                .isEqualTo(255);
        assertThat(commercialAreaStationLineRepository.countByCommercialArea_Source(
                CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS
        )).isEqualTo(242);
    }
}
