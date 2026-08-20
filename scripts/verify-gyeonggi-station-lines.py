#!/usr/bin/env python3
"""Query Kakao SW8 for Gyeonggi station-named commercial areas and emit review CSV."""
from __future__ import annotations
import csv, json, math, os, re, sys
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen

KEY = os.environ["KAKAO_LOCAL_REST_API_KEY"]
EXCLUDED = {"용인역북우편취급국", "낙원역사공원", "역곡로", "역곡북부시장", "역말로"}
ALIASES = {"고양구일산역사": "일산역"}

def distance(a, b, c, d):
    p = math.pi / 180; y = (c-a)*p; x = (d-b)*p
    z = math.sin(y/2)**2 + math.cos(a*p)*math.cos(c*p)*math.sin(x/2)**2
    return round(6371000 * 2 * math.atan2(math.sqrt(z), math.sqrt(1-z)))

def query(value):
    request = Request("https://dapi.kakao.com/v2/local/search/keyword.json?" + urlencode({"query": value, "category_group_code": "SW8", "size": 15}), headers={"Authorization": "KakaoAK " + KEY})
    with urlopen(request) as response: return json.load(response)["documents"]

def main():
    source, areas, output = map(Path, sys.argv[1:4])
    rows = list(csv.DictReader(source.open(encoding="cp949")))
    area_codes = {(row["area_name"], row["latitude"], row["longitude"]): row["external_code"]
                  for row in csv.DictReader(areas.open(encoding="utf-8"), delimiter="\t")}
    out = []
    for row in rows:
        name = row["상권명"]
        latitude, longitude = f"{float(row['위도']):.7f}", f"{float(row['경도']):.7f}"
        code = area_codes[(name, latitude, longitude)]
        if "역" not in name: continue
        candidate = ALIASES.get(name) or (re.search(r"[가-힣]+역", name).group(0) if re.search(r"[가-힣]+역", name) else "")
        if name in EXCLUDED or not candidate:
            out.append([code, name, latitude, longitude, candidate, "", "", "", "", "", "", "", "EXCLUDED", "not a current station area"]); continue
        docs = query(candidate)
        selected = [d for d in docs if d["place_name"].startswith(candidate) and distance(float(row["위도"]), float(row["경도"]), float(d["y"]), float(d["x"])) <= 1100]
        if not selected:
            out.append([code, name, latitude, longitude, candidate, "", "", "", "", "", "", "", "UNRESOLVED", "no nearby SW8 result"]); continue
        for d in selected:
            line = d["category_name"].split(">")[-1].strip().removeprefix("수도권")
            station_name = d["place_name"].rsplit(" ", 1)[0]
            out.append([code, name, latitude, longitude, candidate, station_name, line, d["address_name"], d["y"], d["x"], distance(float(row["위도"]), float(row["경도"]), float(d["y"]), float(d["x"])), "MATCHED_ALIAS" if name in ALIASES else "MATCHED", ""])
    with output.open("w", encoding="utf-8", newline="") as f:
        csv.writer(f).writerow(["commercialAreaCode","commercialAreaName","commercialAreaLatitude","commercialAreaLongitude","stationQuery","stationName","lineName","stationAddress","stationLatitude","stationLongitude","distanceMeters","verificationStatus","note"]); csv.writer(f).writerows(out)

if __name__ == "__main__": main()
