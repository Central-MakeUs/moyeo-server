#!/usr/bin/env python3
"""Create a MySQL seed script from Seoul commercial-area DBF data.

The source Shapefile uses Korea 2000 / Central Belt coordinates (EPSG:5181).
Only development areas (D) and tourist-special areas (U) are recommendation
candidates in the confirmed MVP policy.
"""

from __future__ import annotations

import argparse
import math
import struct
from pathlib import Path


SOURCE = "SEOUL_COMMERCIAL_ANALYSIS"
TYPE_BY_SOURCE_CODE = {"D": "DEVELOPMENT", "U": "TOURIST_SPECIAL"}
EXPECTED_COUNTS = {"DEVELOPMENT": 249, "TOURIST_SPECIAL": 6}


def read_dbf(path: Path) -> list[dict[str, str]]:
    data = path.read_bytes()
    record_count = struct.unpack("<I", data[4:8])[0]
    header_length = struct.unpack("<H", data[8:10])[0]
    record_length = struct.unpack("<H", data[10:12])[0]
    fields: list[tuple[str, int]] = []
    offset = 32
    while data[offset] != 0x0D:
        descriptor = data[offset:offset + 32]
        fields.append((descriptor[:11].split(b"\0")[0].decode("ascii"), descriptor[16]))
        offset += 32

    rows = []
    for index in range(record_count):
        record = data[header_length + index * record_length:header_length + (index + 1) * record_length]
        if record[0] == 0x2A:
            continue
        value_offset = 1
        row: dict[str, str] = {}
        for name, length in fields:
            row[name] = decode_dbf_text(record[value_offset:value_offset + length])
            value_offset += length
        rows.append(row)
    return rows


def decode_dbf_text(value: bytes) -> str:
    text = value.decode("utf-8").strip()
    if any("가" <= character <= "힣" for character in text):
        return text
    try:
        recovered = text.encode("cp1252").decode("cp949")
    except UnicodeError:
        return text
    return recovered if any("가" <= character <= "힣" for character in recovered) else text


def epsg5181_to_wgs84(easting: float, northing: float) -> tuple[float, float]:
    """Inverse transverse-Mercator transform for EPSG:5181 (GRS80)."""
    semi_major_axis = 6_378_137.0
    inverse_flattening = 298.257222101
    flattening = 1 / inverse_flattening
    eccentricity_squared = 2 * flattening - flattening * flattening
    second_eccentricity_squared = eccentricity_squared / (1 - eccentricity_squared)
    central_meridian = math.radians(127.0)
    latitude_of_origin = math.radians(38.0)
    scale_factor = 1.0
    false_easting = 200_000.0
    false_northing = 500_000.0

    def meridional_arc(latitude: float) -> float:
        return semi_major_axis * (
                (1 - eccentricity_squared / 4 - 3 * eccentricity_squared ** 2 / 64 - 5 * eccentricity_squared ** 3 / 256) * latitude
                - (3 * eccentricity_squared / 8 + 3 * eccentricity_squared ** 2 / 32 + 45 * eccentricity_squared ** 3 / 1024) * math.sin(2 * latitude)
                + (15 * eccentricity_squared ** 2 / 256 + 45 * eccentricity_squared ** 3 / 1024) * math.sin(4 * latitude)
                - (35 * eccentricity_squared ** 3 / 3072) * math.sin(6 * latitude)
        )

    meridional_distance = meridional_arc(latitude_of_origin) + (northing - false_northing) / scale_factor
    mu = meridional_distance / (semi_major_axis * (1 - eccentricity_squared / 4 - 3 * eccentricity_squared ** 2 / 64 - 5 * eccentricity_squared ** 3 / 256))
    e1 = (1 - math.sqrt(1 - eccentricity_squared)) / (1 + math.sqrt(1 - eccentricity_squared))
    footprint_latitude = (
            mu
            + (3 * e1 / 2 - 27 * e1 ** 3 / 32) * math.sin(2 * mu)
            + (21 * e1 ** 2 / 16 - 55 * e1 ** 4 / 32) * math.sin(4 * mu)
            + (151 * e1 ** 3 / 96) * math.sin(6 * mu)
            + (1097 * e1 ** 4 / 512) * math.sin(8 * mu)
    )
    sin_footprint = math.sin(footprint_latitude)
    cos_footprint = math.cos(footprint_latitude)
    tangent_squared = math.tan(footprint_latitude) ** 2
    curvature = second_eccentricity_squared * cos_footprint ** 2
    radius = semi_major_axis / math.sqrt(1 - eccentricity_squared * sin_footprint ** 2)
    meridional_radius = semi_major_axis * (1 - eccentricity_squared) / (1 - eccentricity_squared * sin_footprint ** 2) ** 1.5
    distance = (easting - false_easting) / (radius * scale_factor)

    latitude = footprint_latitude - radius * math.tan(footprint_latitude) / meridional_radius * (
            distance ** 2 / 2
            - (5 + 3 * tangent_squared + 10 * curvature - 4 * curvature ** 2 - 9 * second_eccentricity_squared) * distance ** 4 / 24
            + (61 + 90 * tangent_squared + 298 * curvature + 45 * tangent_squared ** 2 - 252 * second_eccentricity_squared - 3 * curvature ** 2) * distance ** 6 / 720
    )
    longitude = central_meridian + (
            distance
            - (1 + 2 * tangent_squared + curvature) * distance ** 3 / 6
            + (5 - 2 * curvature + 28 * tangent_squared - 3 * curvature ** 2 + 8 * second_eccentricity_squared + 24 * tangent_squared ** 2) * distance ** 5 / 120
    ) / cos_footprint
    return math.degrees(latitude), math.degrees(longitude)


def sql_literal(value: str | None) -> str:
    if value is None or value == "":
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def to_seed_rows(rows: list[dict[str, str]]) -> list[tuple[str, ...]]:
    seed_rows = []
    counts = {area_type: 0 for area_type in EXPECTED_COUNTS}
    for row in rows:
        area_type = TYPE_BY_SOURCE_CODE.get(row["TRDAR_SE_C"])
        if area_type is None:
            continue
        latitude, longitude = epsg5181_to_wgs84(float(row["XCNTS_VALU"]), float(row["YDNTS_VALU"]))
        counts[area_type] += 1
        seed_rows.append((
                SOURCE,
                row["TRDAR_CD"],
                area_type,
                row["TRDAR_CD_N"],
                f"{latitude:.7f}",
                f"{longitude:.7f}",
                row["SIGNGU_CD"],
                row["SIGNGU_CD_"],
                row["ADSTRD_CD"],
                row["ADSTRD_CD_"],
        ))
    if counts != EXPECTED_COUNTS:
        raise ValueError(f"Unexpected selected-area counts: {counts}; expected: {EXPECTED_COUNTS}")
    return sorted(seed_rows, key=lambda row: row[1])


def write_sql(seed_rows: list[tuple[str, ...]], output: Path) -> None:
    columns = "source, external_code, area_type, area_name, latitude, longitude, district_code, district_name, administrative_dong_code, administrative_dong_name"
    values = []
    for row in seed_rows:
        source, external_code, area_type, area_name, latitude, longitude, district_code, district_name, dong_code, dong_name = row
        values.append("(" + ", ".join((
                sql_literal(source), sql_literal(external_code), sql_literal(area_type), sql_literal(area_name),
                latitude, longitude, sql_literal(district_code), sql_literal(district_name), sql_literal(dong_code), sql_literal(dong_name)
        )) + ")")
    output.write_text(
            "-- Generated from Seoul commercial-area DBF; do not edit by hand.\n"
            "INSERT INTO commercial_areas (" + columns + ")\nVALUES\n  " + ",\n  ".join(values) + "\n"
            "ON DUPLICATE KEY UPDATE\n"
            "  area_type = VALUES(area_type),\n"
            "  area_name = VALUES(area_name),\n"
            "  latitude = VALUES(latitude),\n"
            "  longitude = VALUES(longitude),\n"
            "  district_code = VALUES(district_code),\n"
            "  district_name = VALUES(district_name),\n"
            "  administrative_dong_code = VALUES(administrative_dong_code),\n"
            "  administrative_dong_name = VALUES(administrative_dong_name);\n",
            encoding="utf-8"
    )


def write_tsv(seed_rows: list[tuple[str, ...]], output: Path) -> None:
    columns = (
        "source", "external_code", "area_type", "area_name", "latitude", "longitude",
        "district_code", "district_name", "administrative_dong_code", "administrative_dong_name"
    )
    output.write_text(
            "\t".join(columns) + "\n" + "".join("\t".join(row) + "\n" for row in seed_rows),
            encoding="utf-8"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dbf", type=Path, help="Seoul commercial-area .dbf file")
    parser.add_argument("sql_output", type=Path, help="Generated MySQL seed .sql file")
    parser.add_argument("tsv_output", type=Path, help="Generated local-profile seed .tsv file")
    args = parser.parse_args()
    seed_rows = to_seed_rows(read_dbf(args.dbf))
    write_sql(seed_rows, args.sql_output)
    write_tsv(seed_rows, args.tsv_output)


if __name__ == "__main__":
    main()
