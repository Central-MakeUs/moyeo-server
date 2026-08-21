#!/usr/bin/env python3
"""Build the Seoul demo core-commercial-area seed and source-scoped sync SQL."""

from __future__ import annotations

import csv
import sys
from pathlib import Path


SOURCE = "SEOUL_COMMERCIAL_ANALYSIS"
EXPECTED_AREA_COUNT = 100
EXPECTED_LINE_COUNT = 157


def sql(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def write_tsv(path: Path, columns: list[str], rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    draft_path, all_areas_path, all_lines_path, area_output, line_output, sql_output = map(Path, sys.argv[1:7])
    selected_codes = {
        row["commercialAreaCode"]
        for row in csv.DictReader(draft_path.open(encoding="utf-8-sig"))
    }
    areas = [row for row in csv.DictReader(all_areas_path.open(encoding="utf-8"), delimiter="\t") if row["external_code"] in selected_codes]
    lines = [row for row in csv.DictReader(all_lines_path.open(encoding="utf-8"), delimiter="\t") if row["commercial_area_external_code"] in selected_codes]
    if len(areas) != EXPECTED_AREA_COUNT or len(lines) != EXPECTED_LINE_COUNT:
        raise ValueError(f"Expected {EXPECTED_AREA_COUNT} areas and {EXPECTED_LINE_COUNT} station lines, found {len(areas)} areas and {len(lines)} lines")

    area_columns = list(areas[0])
    line_columns = list(lines[0])
    write_tsv(area_output, area_columns, areas)
    write_tsv(line_output, line_columns, lines)

    codes = ", ".join(sql(row["external_code"]) for row in areas)
    area_values = ",\n  ".join(
        "(" + ", ".join("NULL" if not row[column] else sql(row[column]) for column in area_columns) + ")"
        for row in areas
    )
    line_statements = "\n".join(
        "INSERT INTO commercial_area_station_lines (commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters) VALUES ((SELECT id FROM commercial_areas WHERE source = "
        + sql(SOURCE) + " AND external_code = " + sql(row["commercial_area_external_code"]) + "), "
        + ", ".join((sql(row["station_name"]), sql(row["line_name"]), sql(row["station_address"]), row["station_latitude"], row["station_longitude"], row["distance_meters"]))
        + ") ON DUPLICATE KEY UPDATE station_address=VALUES(station_address), station_latitude=VALUES(station_latitude), station_longitude=VALUES(station_longitude), distance_meters=VALUES(distance_meters);"
        for row in lines
    )
    sql_output.write_text(
        "-- Seoul demo core-commercial-area seed: 100 manually selected meeting-core areas from the 255-row raw source.\n"
        "START TRANSACTION;\n"
        "DELETE sl FROM commercial_area_station_lines sl JOIN commercial_areas ca ON ca.id = sl.commercial_area_id "
        "WHERE ca.source = " + sql(SOURCE) + ";\n"
        "DELETE FROM commercial_areas WHERE source = " + sql(SOURCE) + " AND external_code NOT IN (" + codes + ");\n"
        "INSERT INTO commercial_areas (" + ", ".join(area_columns) + ") VALUES\n  " + area_values + "\n"
        "ON DUPLICATE KEY UPDATE area_type=VALUES(area_type), area_name=VALUES(area_name), latitude=VALUES(latitude), longitude=VALUES(longitude);\n"
        + line_statements + "\nCOMMIT;\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
