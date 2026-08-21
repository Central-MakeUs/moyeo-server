package com.moyeo.commercial;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CommercialAreaStationLineSeedScriptTest {

    @Test
    void localTsvContainsVerifiedSeoulStationLinesWithoutDuplicates() throws Exception {
        List<String> rows = Files.readAllLines(Path.of("src/main/resources/commercial-area-station-lines-seoul.tsv"));

        assertThat(rows).hasSize(158);
        assertThat(rows.getFirst()).isEqualTo(
                "commercial_area_external_code\tstation_name\tline_name\tstation_address\tstation_latitude\tstation_longitude\tdistance_meters"
        );

        Set<String> keys = new HashSet<>();
        for (String row : rows.subList(1, rows.size())) {
            String[] columns = row.split("\\t", -1);
            assertThat(columns).hasSize(7);
            assertThat(columns[1]).endsWith("역");
            assertThat(columns[2]).isNotBlank();
            assertThat(keys.add(columns[0] + ":" + columns[1] + ":" + columns[2])).isTrue();
        }
        assertThat(keys).hasSize(157);
    }

    @Test
    void schemaAndSeedScriptsLoadVerifiedStationLinesIntoMysqlCompatibleDatabase() throws Exception {
        String commercialAreaSchema = Files.readString(Path.of("scripts/db/2026-07-27-commercial-areas.sql"));
        String commercialAreaSeed = Files.readString(Path.of("scripts/db/2026-07-27-commercial-areas-seoul.sql"));
        String stationLineSchema = Files.readString(Path.of("scripts/db/2026-08-02-commercial-area-station-lines.sql"));
        String stationLineSeed = Files.readString(Path.of("scripts/db/2026-08-02-commercial-area-station-lines-seoul.sql"));

        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:commercial-area-station-line-seed;MODE=MySQL;DB_CLOSE_DELAY=-1"
        ); Statement statement = connection.createStatement()) {
            statement.execute(commercialAreaSchema);
            statement.execute(commercialAreaSeed);
            statement.execute(stationLineSchema);
            String executableSeed = stationLineSeed.replaceFirst("^--.*\\R", "");
            for (String seedStatement : executableSeed.split(";\\R")) {
                if (!seedStatement.isBlank()) {
                    statement.execute(seedStatement);
                }
            }

            try (ResultSet result = statement.executeQuery("select count(*) from commercial_area_station_lines")) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(242);
            }
        }
    }
}
