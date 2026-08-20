#!/usr/bin/env python3
"""Generate deterministic Gyeonggi development-area seed artifacts."""

from __future__ import annotations

import base64
import csv
import hashlib
import sys
from pathlib import Path

SOURCE = "GYEONGGI_DEVELOPMENT_COMMERCIAL"
TYPE = "DEVELOPMENT"
EXPECTED_COUNT = 858


def area_code(name: str, polygon: str) -> str:
    material = f"{name.strip()}\0{polygon.strip()}".encode("utf-8")
    digest = base64.b32encode(hashlib.sha256(material).digest()).decode("ascii").rstrip("=")
    return "GGD-" + digest[:26]


def sql(value: str | None) -> str:
    return "NULL" if not value else "'" + value.replace("'", "''") + "'"


def main() -> None:
    source, sql_output, tsv_output = map(Path, sys.argv[1:4])
    with source.open(encoding="cp949", newline="") as handle:
        input_rows = list(csv.DictReader(handle))
    if len(input_rows) != EXPECTED_COUNT:
        raise ValueError(f"Expected {EXPECTED_COUNT} rows, found {len(input_rows)}")
    rows = []
    for row in input_rows:
        code = area_code(row["상권명"], row["다중지역정보"])
        rows.append((SOURCE, code, TYPE, row["상권명"].strip(), f"{float(row['위도']):.7f}", f"{float(row['경도']):.7f}", "", "", "", ""))
    if len({row[1] for row in rows}) != EXPECTED_COUNT:
        raise ValueError("Generated Gyeonggi area codes are not unique")
    rows.sort(key=lambda row: row[1])
    header = ("source", "external_code", "area_type", "area_name", "latitude", "longitude", "district_code", "district_name", "administrative_dong_code", "administrative_dong_name")
    with tsv_output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)
    values = ["(" + ", ".join(sql(value) if index not in (4, 5) else value for index, value in enumerate(row)) + ")" for row in rows]
    sql_output.write_text("-- Generated from Gyeonggi development-commercial-area CSV; do not edit by hand.\n"
                          "INSERT INTO commercial_areas (" + ", ".join(header) + ")\nVALUES\n  " + ",\n  ".join(values) + "\n"
                          "ON DUPLICATE KEY UPDATE area_type = VALUES(area_type), area_name = VALUES(area_name), latitude = VALUES(latitude), longitude = VALUES(longitude);\n", encoding="utf-8")


if __name__ == "__main__":
    main()
