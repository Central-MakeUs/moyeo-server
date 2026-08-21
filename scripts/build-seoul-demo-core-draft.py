#!/usr/bin/env python3
"""Build a reviewable Seoul demo core-commercial-area shortlist."""

from __future__ import annotations

import csv
import sys
from pathlib import Path


REPRESENTATIVE_COMMERCIAL_CODES = {
    "3120150", "3120098", "3120231", "3120131", "3120117", "3120129", "3120045", "3120158",
    "3120028", "3120161", "3120162", "3120229", "3120005", "3120102", "3120165", "3120072",
    "3120183", "3120104", "3120008", "3120036", "3120218", "3120093",
}
ADDITIONAL_CORE_STATION_CODES = {
    "3120012", "3120046", "3120050", "3120052", "3120003", "3120006", "3120202", "3120222",
}


def main() -> None:
    areas_path, station_lines_path, output_path = map(Path, sys.argv[1:4])
    areas = list(csv.DictReader(areas_path.open(encoding="utf-8"), delimiter="\t"))
    station_codes = {
        row["commercial_area_external_code"]
        for row in csv.DictReader(station_lines_path.open(encoding="utf-8"), delimiter="\t")
    }
    selected = []
    for area in areas:
        code = area["external_code"]
        if area["area_type"] == "TOURIST_SPECIAL":
            reason = "서울 관광특구"
        elif code in ADDITIONAL_CORE_STATION_CODES:
            reason = "대표 단일 노선 역세권"
        elif code in station_codes and len({line["line_name"] for line in csv.DictReader(station_lines_path.open(encoding="utf-8"), delimiter="\t") if line["commercial_area_external_code"] == code}) >= 2:
            reason = "환승역 핵심 역세권"
        elif code in REPRESENTATIVE_COMMERCIAL_CODES:
            reason = "대표 상권·시장·카페거리·대학가"
        else:
            continue
        selected.append({"selectionReason": reason, "reviewDecision": "", "commercialAreaCode": code, "commercialAreaName": area["area_name"], "areaType": area["area_type"], "latitude": area["latitude"], "longitude": area["longitude"]})
    if len(selected) != 100:
        raise ValueError(f"Expected 100 Seoul demo core areas, found {len(selected)}")
    selected.sort(key=lambda row: (row["selectionReason"], row["commercialAreaName"]))
    with output_path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=selected[0].keys())
        writer.writeheader()
        writer.writerows(selected)


if __name__ == "__main__":
    main()
