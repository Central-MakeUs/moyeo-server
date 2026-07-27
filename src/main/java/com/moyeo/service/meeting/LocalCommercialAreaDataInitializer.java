package com.moyeo.service.meeting;

import com.moyeo.domain.commercial.CommercialAreaEntity;
import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaType;
import com.moyeo.repository.commercial.CommercialAreaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
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
public class LocalCommercialAreaDataInitializer implements ApplicationRunner {

    private static final String RESOURCE_PATH = "commercial-areas-seoul.tsv";

    private final CommercialAreaRepository commercialAreaRepository;

    public LocalCommercialAreaDataInitializer(CommercialAreaRepository commercialAreaRepository) {
        this.commercialAreaRepository = commercialAreaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        long existingSourceCount = commercialAreaRepository.countBySource(
                CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS
        );
        if (existingSourceCount == 255) {
            return;
        }
        if (existingSourceCount != 0) {
            throw new IllegalStateException(
                    "Expected 255 Seoul commercial areas but found " + existingSourceCount
            );
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(RESOURCE_PATH).getInputStream(), StandardCharsets.UTF_8
        ))) {
            List<CommercialAreaEntity> areas = reader.lines()
                    .skip(1)
                    .map(this::toEntity)
                    .toList();
            if (areas.size() != 255) {
                throw new IllegalStateException("Expected 255 Seoul commercial areas but found " + areas.size());
            }
            commercialAreaRepository.saveAll(areas);
        }
    }

    private CommercialAreaEntity toEntity(String line) {
        String[] columns = line.split("\\t", -1);
        if (columns.length != 10) {
            throw new IllegalArgumentException("Invalid commercial-area seed row");
        }
        return new CommercialAreaEntity(
                CommercialAreaSource.valueOf(columns[0]),
                columns[1],
                CommercialAreaType.valueOf(columns[2]),
                columns[3],
                new BigDecimal(columns[4]),
                new BigDecimal(columns[5]),
                columns[6],
                columns[7],
                columns[8],
                columns[9]
        );
    }
}
