package com.moyeo.service.meeting;

import com.moyeo.domain.commercial.CommercialAreaEntity;
import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaStationLineEntity;
import com.moyeo.domain.commercial.CommercialAreaType;
import com.moyeo.repository.commercial.CommercialAreaRepository;
import com.moyeo.repository.commercial.CommercialAreaStationLineRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Profile({"local", "dev"})
@Order(Ordered.LOWEST_PRECEDENCE)
public class LocalCommercialAreaStationLineDataInitializer implements ApplicationRunner {

    private static final String RESOURCE_PATH = "commercial-area-station-lines-seoul.tsv";
    private static final int EXPECTED_AREA_COUNT = 100;
    private static final int EXPECTED_LINE_COUNT = 157;

    private final CommercialAreaRepository commercialAreaRepository;
    private final CommercialAreaStationLineRepository stationLineRepository;

    public LocalCommercialAreaStationLineDataInitializer(
            CommercialAreaRepository commercialAreaRepository,
            CommercialAreaStationLineRepository stationLineRepository
    ) {
        this.commercialAreaRepository = commercialAreaRepository;
        this.stationLineRepository = stationLineRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        long existingCount = stationLineRepository.countByCommercialArea_Source(
                CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS
        );
        if (existingCount == EXPECTED_LINE_COUNT) {
            return;
        }
        if (existingCount != 0) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_LINE_COUNT + " Seoul commercial-area station lines but found " + existingCount
            );
        }

        Map<String, CommercialAreaEntity> areasByExternalCode = commercialAreaRepository
                .findAllBySourceAndAreaTypeInOrderByExternalCodeAsc(
                        CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS,
                        List.of(CommercialAreaType.DEVELOPMENT, CommercialAreaType.TOURIST_SPECIAL)
                ).stream()
                .collect(Collectors.toMap(CommercialAreaEntity::getExternalCode, Function.identity()));
        if (areasByExternalCode.size() != EXPECTED_AREA_COUNT) {
            throw new IllegalStateException("Expected " + EXPECTED_AREA_COUNT + " Seoul commercial areas before station-line seeding");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(RESOURCE_PATH).getInputStream(), StandardCharsets.UTF_8
        ))) {
            List<CommercialAreaStationLineEntity> stationLines = reader.lines()
                    .skip(1)
                    .map(line -> toEntity(line, areasByExternalCode))
                    .toList();
            if (stationLines.size() != EXPECTED_LINE_COUNT) {
                throw new IllegalStateException(
                        "Expected " + EXPECTED_LINE_COUNT + " Seoul commercial-area station lines but found " + stationLines.size()
                );
            }
            stationLineRepository.saveAll(stationLines);
        }
    }

    private CommercialAreaStationLineEntity toEntity(String line, Map<String, CommercialAreaEntity> areasByExternalCode) {
        String[] columns = line.split("\\t", -1);
        if (columns.length != 7) {
            throw new IllegalArgumentException("Invalid commercial-area station-line seed row");
        }
        CommercialAreaEntity area = areasByExternalCode.get(columns[0]);
        if (area == null) {
            throw new IllegalArgumentException("Unknown commercial area for station-line seed");
        }
        return new CommercialAreaStationLineEntity(
                area,
                columns[1],
                columns[2],
                columns[3],
                new BigDecimal(columns[4]),
                new BigDecimal(columns[5]),
                Integer.valueOf(columns[6])
        );
    }
}
