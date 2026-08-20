#!/usr/bin/env python3
from __future__ import annotations
import csv, sys
from pathlib import Path
SOURCE="GYEONGGI_DEVELOPMENT_COMMERCIAL"
def q(v): return "'"+v.replace("'","''")+"'"
def main():
  verified, sqlout, tsvout=map(Path,sys.argv[1:4])
  rows=[r for r in csv.DictReader(verified.open(encoding="utf-8")) if r["verificationStatus"].startswith("MATCHED")]
  if not rows: raise ValueError("No verified station rows")
  data=[]
  for r in rows: data.append((r["commercialAreaCode"],r["stationName"],r["lineName"],r["stationAddress"],r["stationLatitude"],r["stationLongitude"],r["distanceMeters"]))
  with tsvout.open("w",encoding="utf-8",newline="") as f:
    w=csv.writer(f,delimiter="\t",lineterminator="\n");w.writerow(("commercial_area_external_code","station_name","line_name","station_address","station_latitude","station_longitude","distance_meters"));w.writerows(data)
  parts=[]
  for code,n,l,a,lat,lon,d in data: parts.append("INSERT INTO commercial_area_station_lines (commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters) VALUES ((SELECT id FROM commercial_areas WHERE source = %s AND external_code = %s), %s, %s, %s, %s, %s, %s) ON DUPLICATE KEY UPDATE station_address=VALUES(station_address), station_latitude=VALUES(station_latitude), station_longitude=VALUES(station_longitude), distance_meters=VALUES(distance_meters);"%(q(SOURCE),q(code),q(n),q(l),q(a),lat,lon,d))
  sqlout.write_text("-- Generated from verified Kakao SW8 results; do not edit by hand.\n"+"\n".join(parts)+"\n",encoding="utf-8")
if __name__=="__main__": main()
