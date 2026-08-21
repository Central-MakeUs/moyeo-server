#!/usr/bin/env python3
"""Classify Gyeonggi development-area names for recommendation curation."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path


ROAD_NAME = re.compile(r"(로|길)(?:_[0-9]+)?$")
REFERENCE_FACILITY_TERMS = (
    "병원", "우체국", "우편취급국", "주민센터", "파출소", "지구대", "경찰서", "소방서", "119", "시청", "구청",
    "읍사무소", "면사무소", "동사무소", "세무서", "법원", "등기소", "보건소", "교육청",
)
COMMERCIAL_TERMS = (
    "백화점", "아울렛", "마트", "쇼핑", "상가", "로데오", "시장", "먹자", "카페", "맛깔",
    "골목", "거리", "CGV", "메가박스", "영화관", "시네마", "애비뉴엘", "홈플러스", "뉴코아",
)
UNIVERSITY_TERMS = ("대학교", "대학가", "대학로")


def key(name: str, latitude: str, longitude: str) -> tuple[str, str, str]:
    return name.strip(), f"{float(latitude):.7f}", f"{float(longitude):.7f}"


def classification(name: str, area_code: str, station_codes: set[str]) -> tuple[str, str]:
    if area_code in station_codes:
        return "KEEP", "카카오 SW8 검증 역세권"
    if ROAD_NAME.search(name):
        return "EXCLUDE", "도로·길 원본 구간명"
    if any(term in name for term in REFERENCE_FACILITY_TERMS):
        return "EXCLUDE", "기준점 성격의 공공·의료·행정 시설명"
    if any(term in name for term in COMMERCIAL_TERMS):
        return "KEEP", "상업시설·시장 계열"
    if any(term in name for term in UNIVERSITY_TERMS):
        return "KEEP", "대학가 계열"
    if "관광" in name or "문화" in name or "예술" in name:
        return "KEEP", "관광·문화 계열"
    return "REVIEW", "명칭만으로 추천 상권 적합성 판단 불가"


def main() -> None:
    raw_path, seed_path, verification_path, all_output, review_output = map(Path, sys.argv[1:6])
    raw_rows = list(csv.DictReader(raw_path.open(encoding="cp949")))
    seed_rows = list(csv.DictReader(seed_path.open(encoding="utf-8"), delimiter="\t"))
    seed_codes = {
        key(row["area_name"], row["latitude"], row["longitude"]): row["external_code"]
        for row in seed_rows
    }
    verification_rows = list(csv.DictReader(verification_path.open(encoding="utf-8")))
    station_codes = {
        row["commercialAreaCode"]
        for row in verification_rows
        if row["verificationStatus"].startswith("MATCHED")
    }

    columns = (
        "recommendedAction", "reason", "reviewDecision", "commercialAreaCode", "commercialAreaName",
        "storeCount", "latitude", "longitude",
    )
    classified = []
    for raw in raw_rows:
        name = raw["상권명"].strip()
        latitude = f"{float(raw['위도']):.7f}"
        longitude = f"{float(raw['경도']):.7f}"
        area_code = seed_codes[key(name, latitude, longitude)]
        action, reason = classification(name, area_code, station_codes)
        classified.append({
            "recommendedAction": action,
            "reason": reason,
            "reviewDecision": "",
            "commercialAreaCode": area_code,
            "commercialAreaName": name,
            "storeCount": raw["점포수"],
            "latitude": latitude,
            "longitude": longitude,
        })

    classified.sort(key=lambda row: (row["recommendedAction"], -int(row["storeCount"]), row["commercialAreaName"]))
    outputs = [(all_output, classified)]
    for action, filename in (("KEEP", "gyeonggi-commercial-area-keep.csv"), ("EXCLUDE", "gyeonggi-commercial-area-exclude.csv"), ("REVIEW", review_output.name)):
        outputs.append((all_output.parent / filename, [row for row in classified if row["recommendedAction"] == action]))
    outputs.append((
        all_output.parent / "gyeonggi-commercial-area-store-count-650-plus.csv",
        sorted(
            (row for row in classified if int(row["storeCount"]) >= 650),
            key=lambda row: -int(row["storeCount"]),
        ),
    ))

    for output, rows in outputs:
        with output.open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=columns)
            writer.writeheader()
            writer.writerows(rows)

    high_store_names = [
        row["commercialAreaName"]
        for row in sorted(classified, key=lambda row: -int(row["storeCount"]))
        if int(row["storeCount"]) >= 650
    ]
    (all_output.parent / "gyeonggi-commercial-area-store-count-650-plus-names.txt").write_text(
        "\n".join(high_store_names) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
