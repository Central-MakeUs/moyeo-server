#!/usr/bin/env python3
"""Create a curation review for the Seoul recommendation seed."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path


ROAD_NAME = re.compile(r"(로|길)(?:_[0-9]+)?$")
REFERENCE_TERMS = (
    "아파트", "병원", "우체국", "주민센터", "파출소", "지구대", "경찰서", "소방서", "시청", "구청",
    "세무서", "법원", "학교", "고교", "초등학교", "터미널", "전화국",
)
INTERSECTION_TERMS = ("교차로", "사거리", "오거리")


def classify(row: dict[str, str], station_codes: set[str]) -> tuple[str, str]:
    name = row["area_name"]
    if row["area_type"] == "TOURIST_SPECIAL":
        return "KEEP", "서울 관광특구"
    if row["external_code"] in station_codes:
        return "KEEP", "카카오 SW8 검증 역세권"
    if ROAD_NAME.search(name):
        return "EXCLUDE", "도로·길 원본 구간명"
    if any(term in name for term in INTERSECTION_TERMS):
        return "EXCLUDE", "교차로·사거리·오거리 기준점"
    if any(term in name for term in REFERENCE_TERMS):
        return "EXCLUDE", "기준점 성격의 시설명"
    return "REVIEW", "대표 상권명 여부 개별 검토 필요"


def write(path: Path, rows: list[dict[str, str]], columns: list[str]) -> None:
    with path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    area_path, line_path, output_dir = map(Path, sys.argv[1:4])
    areas = list(csv.DictReader(area_path.open(encoding="utf-8"), delimiter="\t"))
    lines = list(csv.DictReader(line_path.open(encoding="utf-8"), delimiter="\t"))
    station_codes = {row["commercial_area_external_code"] for row in lines}
    columns = ["recommendedAction", "reason", "reviewDecision", "commercialAreaCode", "commercialAreaName", "areaType", "latitude", "longitude"]
    result = []
    for area in areas:
        action, reason = classify(area, station_codes)
        result.append({
            "recommendedAction": action,
            "reason": reason,
            "reviewDecision": "",
            "commercialAreaCode": area["external_code"],
            "commercialAreaName": area["area_name"],
            "areaType": area["area_type"],
            "latitude": area["latitude"],
            "longitude": area["longitude"],
        })
    result.sort(key=lambda row: (row["recommendedAction"], row["commercialAreaName"]))
    output_dir.mkdir(parents=True, exist_ok=True)
    write(output_dir / "seoul-commercial-area-classification.csv", result, columns)
    for action, filename in (("KEEP", "seoul-commercial-area-keep.csv"), ("EXCLUDE", "seoul-commercial-area-exclude.csv"), ("REVIEW", "seoul-commercial-area-review.csv")):
        write(output_dir / filename, [row for row in result if row["recommendedAction"] == action], columns)


if __name__ == "__main__":
    main()
