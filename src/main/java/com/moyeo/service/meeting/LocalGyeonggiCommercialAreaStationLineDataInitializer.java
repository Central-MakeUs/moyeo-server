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
public class LocalGyeonggiCommercialAreaStationLineDataInitializer implements ApplicationRunner {
    private static final int EXPECTED_COUNT = 15;

    private final CommercialAreaRepository areaRepository;
    private final CommercialAreaStationLineRepository lineRepository;

    public LocalGyeonggiCommercialAreaStationLineDataInitializer(
            CommercialAreaRepository areaRepository,
            CommercialAreaStationLineRepository lineRepository
    ) {
        this.areaRepository = areaRepository;
        this.lineRepository = lineRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        long count = lineRepository.countByCommercialArea_Source(CommercialAreaSource.GYEONGGI_DEVELOPMENT_COMMERCIAL);
        if (count == EXPECTED_COUNT) {
            return;
        }
        if (count != 0) {
            throw new IllegalStateException("Expected 15 curated Gyeonggi station lines but found " + count);
        }

        Map<String, CommercialAreaEntity> areas = areaRepository
                .findAllBySourceAndAreaTypeInOrderByExternalCodeAsc(
                        CommercialAreaSource.GYEONGGI_DEVELOPMENT_COMMERCIAL,
                        List.of(CommercialAreaType.DEVELOPMENT)
                ).stream()
                .collect(Collectors.toMap(CommercialAreaEntity::getExternalCode, Function.identity()));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("commercial-area-station-lines-gyeonggi.tsv").getInputStream(), StandardCharsets.UTF_8
        ))) {
            List<CommercialAreaStationLineEntity> lines = reader.lines().skip(1).map(line -> entity(line, areas)).toList();
            if (lines.size() != EXPECTED_COUNT) {
                throw new IllegalStateException("Expected 118 Gyeonggi station lines but found " + lines.size());
            }
            lineRepository.saveAll(lines);
        }
    }

    private CommercialAreaStationLineEntity entity(String line, Map<String, CommercialAreaEntity> areas) {
        String[] columns = line.split("\\t", -1);
        if (columns.length != 7) {
            throw new IllegalArgumentException("Invalid Gyeonggi station-line seed row");
        }
        CommercialAreaEntity area = areas.get(columns[0]);
        if (area == null) {
            throw new IllegalArgumentException("Unknown Gyeonggi commercial area for station-line seed");
        }
        return new CommercialAreaStationLineEntity(
                area,
                columns[1],
                columns[2],
                columns[3],
                new BigDecimal(columns[4]),
                new BigDecimal(columns[5]),
                Integer.parseInt(columns[6])
        );
    }
}
