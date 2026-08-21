package com.moyeo.service.meeting;

import com.moyeo.domain.commercial.CommercialAreaEntity;
import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaType;
import com.moyeo.repository.commercial.CommercialAreaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Profile({"local", "dev"})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalGyeonggiCommercialAreaDataInitializer implements ApplicationRunner {
    private static final int EXPECTED_COUNT = 20;

    private final CommercialAreaRepository repository;

    public LocalGyeonggiCommercialAreaDataInitializer(CommercialAreaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        long count = repository.countBySource(CommercialAreaSource.GYEONGGI_DEVELOPMENT_COMMERCIAL);
        if (count == EXPECTED_COUNT) {
            return;
        }
        if (count != 0) {
            throw new IllegalStateException("Expected 20 curated Gyeonggi commercial areas but found " + count);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("commercial-areas-gyeonggi.tsv").getInputStream(), StandardCharsets.UTF_8
        ))) {
            List<CommercialAreaEntity> areas = reader.lines().skip(1).map(this::toEntity).toList();
            if (areas.size() != EXPECTED_COUNT) {
                throw new IllegalStateException("Expected 20 curated Gyeonggi commercial areas but found " + areas.size());
            }
            repository.saveAll(areas);
        }
    }

    private CommercialAreaEntity toEntity(String line) {
        String[] c = line.split("\\t", -1);
        if (c.length != 10) throw new IllegalArgumentException("Invalid Gyeonggi commercial-area seed row");
        return new CommercialAreaEntity(
                CommercialAreaSource.valueOf(c[0]),
                c[1],
                CommercialAreaType.valueOf(c[2]),
                c[3],
                new BigDecimal(c[4]),
                new BigDecimal(c[5]),
                c[6],
                c[7],
                c[8],
                c[9]
        );
    }
}
