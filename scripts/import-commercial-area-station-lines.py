#!/usr/bin/env python3
"""Generate commercial-area station-line seed artifacts from verified Kakao results."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path


SOURCE = "SEOUL_COMMERCIAL_ANALYSIS"
EXPECTED_LINE_COUNT = 242


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as source:
        rows = list(csv.DictReader(source))
    if len(rows) != EXPECTED_LINE_COUNT:
        raise ValueError(f"Expected {EXPECTED_LINE_COUNT} verified station-line rows, found {len(rows)}")
    for row in rows:
        if row["verificationStatus"] not in {"MATCHED", "MATCHED_ALIAS"}:
            raise ValueError("Station-line seed contains an unresolved verification row")
        if row["apiFinalPageIsEnd"] != "True":
            raise ValueError("Station-line seed contains an incomplete API page traversal")
    return rows


def write_tsv(rows: list[dict[str, str]], output: Path) -> None:
    columns = (
        "commercial_area_external_code", "station_name", "line_name", "station_address",
        "station_latitude", "station_longitude", "distance_meters",
    )
    with output.open("w", encoding="utf-8", newline="") as target:
        writer = csv.writer(target, delimiter="\t", lineterminator="\n")
        writer.writerow(columns)
        for row in rows:
            writer.writerow((
                row["commercialAreaCode"], row["stationName"], row["lineName"], row["apiAddressName"],
                row["stationLatitude"], row["stationLongitude"], row["distanceMeters"],
            ))


def write_sql(rows: list[dict[str, str]], output: Path) -> None:
    statements = ["-- Generated from verified Kakao SW8 station-line results; do not edit by hand."]
    for row in rows:
        statements.extend((
            "INSERT INTO commercial_area_station_lines (",
            "  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters",
            ")",
            "SELECT id, " + ", ".join((
                sql_literal(row["stationName"]), sql_literal(row["lineName"]), sql_literal(row["apiAddressName"]),
                row["stationLatitude"], row["stationLongitude"], row["distanceMeters"],
            )),
            "FROM commercial_areas",
            "WHERE source = " + sql_literal(SOURCE) + " AND external_code = " + sql_literal(row["commercialAreaCode"]),
            "ON DUPLICATE KEY UPDATE",
            "  station_address = VALUES(station_address),",
            "  station_latitude = VALUES(station_latitude),",
            "  station_longitude = VALUES(station_longitude),",
            "  distance_meters = VALUES(distance_meters);",
            "",
        ))
    output.write_text("\n".join(statements), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("verification_csv", type=Path)
    parser.add_argument("sql_output", type=Path)
    parser.add_argument("tsv_output", type=Path)
    args = parser.parse_args()

    rows = read_rows(args.verification_csv)
    write_sql(rows, args.sql_output)
    write_tsv(rows, args.tsv_output)


if __name__ == "__main__":
    main()
