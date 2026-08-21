#!/usr/bin/env python3
"""Build the curated Gyeonggi recommendation seed from the reviewed classification."""

from __future__ import annotations

import csv
import sys
from pathlib import Path


SOURCE = "GYEONGGI_DEVELOPMENT_COMMERCIAL"
MINIMUM_STORE_COUNT = 650


def sql(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def main() -> None:
    classification_path, all_areas_path, all_lines_path, area_output, line_output, sql_output = map(Path, sys.argv[1:7])
    classification = list(csv.DictReader(classification_path.open(encoding="utf-8-sig")))
    selected_codes = {
        row["commercialAreaCode"]
        for row in classification
        if row["recommendedAction"] == "KEEP" and int(row["storeCount"]) >= MINIMUM_STORE_COUNT
    }
    areas = [row for row in csv.DictReader(all_areas_path.open(encoding="utf-8"), delimiter="\t") if row["external_code"] in selected_codes]
    lines = [row for row in csv.DictReader(all_lines_path.open(encoding="utf-8"), delimiter="\t") if row["commercial_area_external_code"] in selected_codes]
    if len(areas) != 20 or len(lines) != 15:
        raise ValueError(f"Expected 20 areas and 15 station lines, found {len(areas)} areas and {len(lines)} lines")

    area_columns = list(areas[0])
    line_columns = list(lines[0])
    for path, columns, rows in ((area_output, area_columns, areas), (line_output, line_columns, lines)):
        with path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=columns, delimiter="\t", lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)

    codes = ", ".join(sql(row["external_code"]) for row in areas)
    area_values = ",\n  ".join("(" + ", ".join(
        "NULL" if not row[column] else sql(row[column])
        for column in area_columns
    ) + ")" for row in areas)
    line_statements = "\n".join(
        "INSERT INTO commercial_area_station_lines (commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters) VALUES ((SELECT id FROM commercial_areas WHERE source = "
        + sql(SOURCE) + " AND external_code = " + sql(row["commercial_area_external_code"]) + "), "
        + ", ".join((sql(row["station_name"]), sql(row["line_name"]), sql(row["station_address"]), row["station_latitude"], row["station_longitude"], row["distance_meters"]))
        + ") ON DUPLICATE KEY UPDATE station_address=VALUES(station_address), station_latitude=VALUES(station_latitude), station_longitude=VALUES(station_longitude), distance_meters=VALUES(distance_meters);"
        for row in lines
    )
    sql_output.write_text(
        "-- Curated Gyeonggi recommendation seed: KEEP classification with storeCount >= 650.\n"
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
