package com.moyeo.commercial;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CommercialAreaSeedScriptTest {

    private static final Pattern ROW_PATTERN = Pattern.compile(
            "\\('SEOUL_COMMERCIAL_ANALYSIS', '[^']+', '(DEVELOPMENT|TOURIST_SPECIAL)', '[^']+', (37\\.[0-9]+), (12[67]\\.[0-9]+),"
    );
    private static final Pattern AREA_KEY_PATTERN = Pattern.compile(
            "\\('SEOUL_COMMERCIAL_ANALYSIS', '([^']+)', '(DEVELOPMENT|TOURIST_SPECIAL)', '((?:''|[^'])+)',"
    );

    @Test
    void seedScriptContainsConfirmedSeoulCommercialAreasWithWgs84Coordinates() throws Exception {
        String sql = Files.readString(Path.of("scripts/db/2026-07-27-commercial-areas-seoul.sql"));
        Matcher matcher = ROW_PATTERN.matcher(sql);
        int developmentCount = 0;
        int touristSpecialCount = 0;

        while (matcher.find()) {
            if ("DEVELOPMENT".equals(matcher.group(1))) {
                developmentCount++;
            } else {
                touristSpecialCount++;
            }
        }

        assertThat(developmentCount).isEqualTo(249);
        assertThat(touristSpecialCount).isEqualTo(6);
        assertThat(sql).contains("ON DUPLICATE KEY UPDATE");
    }

    @Test
    void schemaAndSeedScriptsLoadAllCommercialAreasIntoMysqlCompatibleDatabase() throws Exception {
        String schema = Files.readString(Path.of("scripts/db/2026-07-27-commercial-areas.sql"));
        String seed = Files.readString(Path.of("scripts/db/2026-07-27-commercial-areas-seoul.sql"));

        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:commercial-area-seed;MODE=MySQL;DB_CLOSE_DELAY=-1"
        ); Statement statement = connection.createStatement()) {
            statement.execute(schema);
            statement.execute(seed);

            try (ResultSet result = statement.executeQuery(
                    "select area_type, count(*) from commercial_areas group by area_type order by area_type"
            )) {
                result.next();
                assertThat(result.getString(1)).isEqualTo("DEVELOPMENT");
                assertThat(result.getInt(2)).isEqualTo(249);
                result.next();
                assertThat(result.getString(1)).isEqualTo("TOURIST_SPECIAL");
                assertThat(result.getInt(2)).isEqualTo(6);
                assertThat(result.next()).isFalse();
            }
        }
    }

    @Test
    void localTsvAndProductionSqlContainTheSameCommercialAreas() throws Exception {
        String sql = Files.readString(Path.of("scripts/db/2026-07-27-commercial-areas-seoul.sql"));
        Set<String> sqlAreaKeys = new HashSet<>();
        Matcher matcher = AREA_KEY_PATTERN.matcher(sql);
        while (matcher.find()) {
            sqlAreaKeys.add(matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(3).replace("''", "'"));
        }

        Set<String> tsvAreaKeys = Files.readAllLines(Path.of("src/main/resources/commercial-areas-seoul.tsv"))
                .stream()
                .skip(1)
                .map(line -> line.split("\\t", -1))
                .map(columns -> columns[1] + ":" + columns[2] + ":" + columns[3])
                .collect(java.util.stream.Collectors.toSet());

        assertThat(tsvAreaKeys).hasSize(255).isEqualTo(sqlAreaKeys);
    }

    @Test
    void generatedGangnamStationCoordinateMatchesKnownWgs84Area() throws Exception {
        String row = Files.readAllLines(Path.of("src/main/resources/commercial-areas-seoul.tsv"))
                .stream()
                .filter(line -> line.contains("\t3120189\tDEVELOPMENT\t강남역\t"))
                .findFirst()
                .orElseThrow();
        String[] columns = row.split("\\t", -1);

        assertThat(new java.math.BigDecimal(columns[4]))
                .isBetween(new java.math.BigDecimal("37.4970"), new java.math.BigDecimal("37.4990"));
        assertThat(new java.math.BigDecimal(columns[5]))
                .isBetween(new java.math.BigDecimal("127.0270"), new java.math.BigDecimal("127.0290"));
    }
}
