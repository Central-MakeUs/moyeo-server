-- Generated from verified Kakao SW8 station-line results; do not edit by hand.
INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서대문역', '5호선', '서울 종로구 평동 210', 37.56575193662503, 126.96663755850754, 114
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120001'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '경복궁역', '3호선', '서울 종로구 적선동 81-1', 37.5758061234865, 126.973691352767, 420
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120002'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '광화문역', '5호선', '서울 종로구 세종로 1-68', 37.57164860977568, 126.97642427981408, 165
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120003'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '종각역', '1호선', '서울 종로구 종로1가 54', 37.570227990912244, 126.98315081716676, 35
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120006'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '안국역', '3호선', '서울 종로구 안국동 73-1', 37.57650192060523, 126.98542143228659, 297
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120007'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '종로3가역', '3호선', '서울 종로구 묘동 20-5', 37.571563287751246, 126.9918757981544, 105
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120009'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '종로3가역', '5호선', '서울 종로구 돈의동 39-1', 37.57258941181964, 126.9904176797898, 107
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120009'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '종로3가역', '1호선', '서울 종로구 종로3가 10-5', 37.570420844523, 126.992153252476, 188
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120009'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '혜화역', '4호선', '서울 종로구 명륜4가 96-5', 37.58204391787134, 127.00194500977393, 95
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120012'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '종로5가역', '1호선', '서울 종로구 종로5가 82-1', 37.57097610838373, 127.00153834521934, 124
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120014'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '동대문역', '4호선', '서울 종로구 종로6가 287-1', 37.5709072753658, 127.009313828405, 185
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120016'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '동대문역', '1호선', '서울 종로구 창신동 492-1', 37.57166940579655, 127.0106338108367, 82
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120016'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '동묘앞역', '6호선', '서울 종로구 숭인동 316-3', 37.5721715926337, 127.015725557994, 114
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120017'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '동묘앞역', '1호선', '서울 종로구 숭인동 117', 37.5732166600283, 127.016364227103, 160
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120017'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '시청역', '2호선', '서울 중구 서소문동 90-1', 37.56368183746611, 126.97559827045151, 105
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120020'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '시청역', '1호선', '서울 중구 정동 5-5', 37.56534539636417, 126.97719821079865, 269
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120020'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '시청역', '2호선', '서울 중구 서소문동 90-1', 37.56368183746611, 126.97559827045151, 245
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120021'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '시청역', '1호선', '서울 중구 정동 5-5', 37.56534539636417, 126.97719821079865, 476
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120021'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '시청역', '2호선', '서울 중구 서소문동 90-1', 37.56368183746611, 126.97559827045151, 195
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120022'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '시청역', '1호선', '서울 중구 정동 5-5', 37.56534539636417, 126.97719821079865, 261
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120022'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '회현역', '4호선', '서울 중구 남창동 64-1', 37.55876114587941, 126.9784372569283, 166
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120024'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '을지로입구역', '2호선', '서울 중구 을지로1가 100-1', 37.566035517712955, 126.9821953112953, 186
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120026'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '명동역', '4호선', '서울 중구 충무로2가 109-2', 37.56096526943837, 126.98640235001736, 266
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120027'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '을지로3가역', '2호선', '서울 중구 을지로3가 347-3', 37.56629149790628, 126.99098443539428, 56
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120031'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '을지로3가역', '3호선', '서울 중구 을지로3가 282-8', 37.5664312655685, 126.992615486076, 91
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120031'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '충무로역', '4호선', '서울 중구 필동2가 16-2', 37.56151106015413, 126.9952553883253, 30
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120032'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '충무로역', '3호선', '서울 중구 필동2가 16-2', 37.56139658395457, 126.99414960395544, 70
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120032'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '을지로4가역', '5호선', '서울 중구 주교동 191', 37.567564032148596, 126.99804517954063, 141
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120033'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '을지로4가역', '2호선', '서울 중구 을지로4가 267-1', 37.5666405038268, 126.997632059113, 86
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120033'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '동대문역사문화공원역', '2호선', '서울 중구 을지로7가 112-3', 37.56566440553802, 127.00900417014896, 145
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120037'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '동대문역사문화공원역', '5호선', '서울 중구 광희동1가 235-6', 37.56462215427451, 127.00579179092047, 168
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120037'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '동대문역사문화공원역', '4호선', '서울 중구 광희동2가 9', 37.565033800640144, 127.00769790634796, 55
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120037'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '약수역', '6호선', '서울 중구 신당동 369-2', 37.5539252226626, 127.010153707232, 41
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120038'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '약수역', '3호선', '서울 중구 신당동 369-44', 37.5545053994984, 127.010891666957, 80
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120038'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신당역', '2호선', '서울 중구 신당동 99', 37.5656730531732, 127.019477533278, 332
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120039'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신당역', '6호선', '서울 중구 흥인동 162-1', 37.56609251486158, 127.01617590593352, 84
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120039'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '용산역', '1호선', '서울 용산구 한강로3가 40-999', 37.52977356999725, 126.96462961258051, 474
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120040'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '용산역', '경의중앙선', '서울 용산구 한강로3가 40-999', 37.52960692295435, 126.96475753303035, 496
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120040'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '용산역', '1호선', '서울 용산구 한강로3가 40-999', 37.52977356999725, 126.96462961258051, 143
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120041'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '용산역', '경의중앙선', '서울 용산구 한강로3가 40-999', 37.52960692295435, 126.96475753303035, 143
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120041'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '남영역', '1호선', '서울 용산구 갈월동 96-1', 37.540566672483216, 126.97133096608212, 191
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120042'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서울역', 'GTX-A', '서울 중구 봉래동2가 166-1', 37.5554875216741, 126.972636930856, 100
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120043'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서울역', '경의중앙선', '서울 중구 봉래동2가 122-28', 37.55693330961779, 126.9713179064219, 157
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120043'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서울역', '4호선', '서울 용산구 동자동 14-151', 37.553027798935524, 126.97264235696984, 371
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120043'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서울역', '공항철도', '서울 용산구 동자동 43-205', 37.55332892758497, 126.96974961781686, 438
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120043'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서울역', '1호선', '서울 중구 남대문로5가 73-6', 37.55597933890212, 126.97209238331357, 86
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120043'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '삼각지역', '6호선', '서울 용산구 한강로1가 228-1', 37.53560639488231, 126.97405509443837, 200
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120044'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '삼각지역', '4호선', '서울 용산구 한강로1가 228-1', 37.5344393447708, 126.972922951307, 66
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120044'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '이태원역', '6호선', '서울 용산구 이태원동 119-23', 37.5345252050511, 126.994333861918, 60
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120046'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '금호역', '3호선', '서울 성동구 금호동4가 1470', 37.548260922874086, 127.01582916945283, 335
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120048'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '왕십리역', '5호선', '서울 성동구 행당동 192', 37.56203954461276, 127.03733125702163, 126
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120049'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '왕십리역', '수인분당선', '서울 성동구 행당동 168-151', 37.56063982853407, 127.03878380532441, 143
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120049'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '왕십리역', '경의중앙선', '서울 성동구 행당동 168-151', 37.56205814456861, 127.0383329389645, 162
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120049'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '왕십리역', '2호선', '서울 성동구 행당동 192', 37.561268363317176, 127.03710337610202, 41
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120049'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서울숲역', '수인분당선', '서울 성동구 성수동1가 656-436', 37.543645796605, 127.044746216358, 253
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120050'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '뚝섬역', '2호선', '서울 성동구 성수동1가 656-745', 37.547241554679, 127.04738727881, 111
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120051'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '성수역', '2호선', '서울 성동구 성수동2가 300-1', 37.5445888153751, 127.056066999327, 54
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120052'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '건대입구역', '7호선', '서울 광진구 화양동 6-3', 37.5408686005702, 127.071104284458, 154
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120053'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '건대입구역', '2호선', '서울 광진구 화양동 7-3', 37.54040751726388, 127.06920291650829, 23
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120053'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '어린이대공원역', '7호선', '서울 광진구 화양동 164-1', 37.54796087622509, 127.07465525507352, 217
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120054'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '군자역', '5호선', '서울 광진구 능동 275-5', 37.557151488837924, 127.0795106156054, 15
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120055'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '군자역', '7호선', '서울 광진구 능동 275-5', 37.55745501860533, 127.07966825326926, 46
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120055'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '구의역', '2호선', '서울 광진구 구의동 245-24', 37.5371752725594, 127.086180837795, 119
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120056'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '아차산역', '5호선', '서울 광진구 능동 256-16', 37.55224917071854, 127.08956495447913, 319
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120057'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '강변역', '2호선', '서울 광진구 구의동 546-6', 37.5351180385975, 127.094741101863, 35
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120060'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신설동역', '1호선', '서울 동대문구 신설동 76-5', 37.5760299683175, 127.024456700382, 120
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120062'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신설동역', '우이신설선', '서울 동대문구 신설동 76-5', 37.576550995373644, 127.02320708440578, 211
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120062'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신설동역', '2호선', '서울 동대문구 신설동 76-5', 37.5748045192005, 127.024918166399, 41
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120062'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '청량리역', '수인분당선', '서울 동대문구 전농동 547', 37.5808953806814, 127.047847862522, 13
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120063'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '청량리역', '1호선', '서울 동대문구 전농동 620-67', 37.580037056302906, 127.04472723023305, 292
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120063'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '청량리역', '경춘선', '서울 동대문구 전농동 547', 37.581273475892466, 127.0486360630067, 79
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120063'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '청량리역', '경의중앙선', '서울 동대문구 전농동 547', 37.580279093795085, 127.04786558187519, 81
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120063'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '회기역', '경의중앙선', '서울 동대문구 휘경동 317-112', 37.5895296349726, 127.05782624137, 22
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120065'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '회기역', '1호선', '서울 동대문구 휘경동 317-112', 37.5897962196601, 127.058048369273, 57
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120065'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '회기역', '경춘선', '서울 동대문구 휘경동 317-112', 37.5899221169641, 127.058537601316, 98
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120065'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '장한평역', '5호선', '서울 동대문구 장안동 472-3', 37.5616038910656, 127.06370373106944, 92
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120066'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '사가정역', '7호선', '서울 중랑구 면목동 495', 37.5809403751459, 127.088504554212, 38
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120068'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '상봉역', '7호선', '서울 중랑구 상봉동 100-9', 37.5955853328943, 127.085754455388, 197
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120069'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '상봉역', '경의중앙선', '서울 중랑구 상봉동 100-9', 37.59671381067542, 127.08515107127755, 233
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120069'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '상봉역', '경춘선', '서울 중랑구 상봉동 100-9', 37.5969733432502, 127.085085690038, 245
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120069'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '한성대입구역', '4호선', '서울 성북구 삼선동1가 14', 37.58842461354086, 127.00601781685579, 207
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120070'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '안암역', '6호선', '서울 성북구 안암동5가 146-1', 37.586307417971, 127.029233420248, 58
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120074'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '월곡역', '6호선', '서울 성북구 하월곡동 35-1', 37.6018044172918, 127.041460725816, 161
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120075'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '수유역', '4호선', '서울 강북구 수유동 140', 37.63788539420793, 127.02550910860451, 48
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120076'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '미아역', '4호선', '서울 강북구 미아동 194-1', 37.6266544891889, 127.026041090189, 110
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120077'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '미아사거리역', '4호선', '서울 강북구 미아동 66-1', 37.61327836400571, 127.03008663628454, 106
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120078'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '창동역', '1호선', '서울 도봉구 창동 135-1', 37.6533385121404, 127.047644999036, 105
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120079'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '창동역', '4호선', '서울 도봉구 창동 135-1', 37.6533706449195, 127.04838841056, 125
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120079'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '마들역', '7호선', '서울 노원구 상계동 650', 37.6652017465321, 127.057712619869, 84
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120080'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '노원역', '4호선', '서울 노원구 상계동 602-5', 37.6563403513278, 127.063449137455, 148
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120081'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '노원역', '7호선', '서울 노원구 상계동 729', 37.6545110763411, 127.06055556456411, 178
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120081'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '공릉역', '7호선', '서울 노원구 공릉동 385-4', 37.6255427648558, 127.073033585328, 56
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120084'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '태릉입구역', '7호선', '서울 노원구 공릉동 616-4', 37.618459513199, 127.075405300578, 160
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120086'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '태릉입구역', '6호선', '서울 노원구 공릉동 616-4', 37.6173499119222, 127.07475176155, 281
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120086'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '응암역', '6호선', '서울 은평구 신사동 22-15', 37.5984928632876, 126.9155399209037, 38
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120087'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '구산역', '6호선', '서울 은평구 구산동 1', 37.61124669148435, 126.91725947715172, 79
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120088'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '구파발역', '3호선', '서울 은평구 진관동 67-26', 37.6364357889145, 126.918852820009, 100
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120089'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '연신내역', 'GTX-A', '서울 은평구 갈현동 389-15', 37.6189362438035, 126.920637714408, 127
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120090'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '연신내역', '6호선', '서울 은평구 갈현동 397', 37.61851445829909, 126.92045466718463, 155
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120090'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '연신내역', '3호선', '서울 은평구 갈현동 397', 37.61920414913163, 126.92110296894133, 87
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120090'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '불광역', '3호선', '서울 은평구 대조동 13-10', 37.6100477979795, 126.930315826401, 177
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120092'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '불광역', '6호선', '서울 은평구 대조동 13-10', 37.61087796280672, 126.92939540272262, 252
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120092'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신촌역', '2호선', '서울 마포구 노고산동 31-11', 37.555198169366435, 126.93698075993808, 156
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120094'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신촌역', '경의중앙선', '서울 서대문구 신촌동 74-12', 37.55967513765615, 126.94208931819857, 610
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120094'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '홍제역', '3호선', '서울 서대문구 홍제동 161-1', 37.5887953806088, 126.944158007985, 82
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120095'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '이대역', '2호선', '서울 마포구 염리동 8-85', 37.556814718869, 126.94642954546576, 371
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120096'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '충정로역', '2호선', '서울 서대문구 충정로3가 319-1', 37.55976328822766, 126.96449207476172, 148
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120097'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '충정로역', '5호선', '서울 서대문구 충정로3가 248-1', 37.56042867918723, 126.96303964820936, 7
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120097'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '월드컵경기장역', '6호선', '서울 마포구 성산동 420', 37.5696355653344, 126.899098148061, 139
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120099'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '망원역', '6호선', '서울 마포구 망원동 378', 37.5560826563712, 126.910094329982, 87
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120100'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '합정역', '2호선', '서울 마포구 서교동 393', 37.54991315995173, 126.91445406513526, 99
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120101'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '합정역', '6호선', '서울 마포구 합정동 414', 37.5487840343032, 126.91421318621, 99
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120101'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '홍대입구역', '공항철도', '서울 마포구 동교동 190-1', 37.5573052656667, 126.927010430346, 1043
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120103'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '홍대입구역', '경의중앙선', '서울 마포구 동교동 190-1', 37.5574494922411, 126.927118938976, 1062
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120103'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '홍대입구역', '2호선', '서울 마포구 동교동 165', 37.5568707448873, 126.923778562273, 829
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120103'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '상수역', '6호선', '서울 마포구 상수동 309-10', 37.54776618366393, 126.92242989321456, 92
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120105'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '마포역', '5호선', '서울 마포구 도화동 160', 37.53955402908912, 126.94586257221307, 45
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120106'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '공덕역', '5호선', '서울 마포구 공덕동 423-29', 37.5445416897534, 126.951451177648, 199
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120107'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '공덕역', '경의중앙선', '서울 마포구 도화동 25-13', 37.5424735099283, 126.95271984059, 75
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120107'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '공덕역', '공항철도', '서울 마포구 도화동 25-13', 37.5423148868697, 126.952602261123, 77
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120107'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '공덕역', '6호선', '서울 마포구 공덕동 439', 37.543530049331636, 126.95189540256291, 81
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120107'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신정네거리역', '2호선', '서울 양천구 신정동 1231', 37.5202231231721, 126.852889567642, 182
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120108'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '목동역', '5호선', '서울 양천구 목동 926-3', 37.526135115923466, 126.86462527157516, 278
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120110'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '등촌역', '9호선', '서울 강서구 등촌동 666-94', 37.550728980328884, 126.86557100595411, 78
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120112'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '오목교역', '5호선', '서울 양천구 목동 406-30', 37.5245340839144, 126.875307312696, 132
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120113'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '김포공항역', '김포골드라인', '서울 강서구 방화동 886', 37.56232990369519, 126.80199188908921, 513
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120115'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '김포공항역', '5호선', '서울 강서구 방화동 886', 37.56209981580658, 126.80125680693868, 542
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120115'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '김포공항역', '9호선', '서울 강서구 방화동 886', 37.56196757232288, 126.80191361988808, 553
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120115'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '김포공항역', '공항철도', '서울 강서구 방화동 886', 37.561829756420224, 126.8024731079849, 570
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120115'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '김포공항역', '서해선', '서울 강서구 방화동 886', 37.5617956120178, 126.804151698874, 602
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120115'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '송정역', '5호선', '서울 강서구 공항동 29-5', 37.5611612387718, 126.812430347154, 249
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120116'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '마곡역', '5호선', '서울 강서구 마곡동 769-3', 37.56022863776301, 126.82469691791624, 195
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120118'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '발산역', '5호선', '서울 강서구 마곡동 727-1496', 37.558677743241596, 126.83773640693936, 228
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120119'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '화곡역', '5호선', '서울 강서구 화곡동 1089-54', 37.5416507925709, 126.840455026335, 157
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120120'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '까치산역', '2호선', '서울 강서구 화곡동 662-5', 37.5314060103327, 126.846948282657, 144
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120121'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '까치산역', '5호선', '서울 강서구 화곡동 662-5', 37.5322306724471, 126.846444264065, 245
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120121'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '오류동역', '1호선', '서울 구로구 오류동 66-10', 37.49439720933549, 126.84480744815397, 94
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120123'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '구로역', '1호선', '서울 구로구 구로동 585-3', 37.50334155631282, 126.88230806229326, 395
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120125'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신도림역', '2호선', '서울 구로구 신도림동 460-26', 37.508222191535346, 126.89161095407636, 108
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120128'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신도림역', '1호선', '서울 구로구 신도림동 460-26', 37.508908482648, 126.891312500851, 114
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120128'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '구로디지털단지역', '2호선', '서울 구로구 구로동 810-3', 37.4852605752505, 126.901473080039, 230
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120130'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '양평역', '5호선', '서울 영등포구 양평동2가 33-79', 37.5255796429707, 126.886419282885, 219
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120135'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '영등포구청역', '5호선', '서울 영등포구 당산동3가 556-1', 37.5242291583027, 126.895322094259, 257
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120136'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '영등포구청역', '2호선', '서울 영등포구 당산동3가 270-1', 37.5258305311402, 126.89667739939, 344
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120136'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '문래역', '2호선', '서울 영등포구 문래동3가 68-1', 37.5179757181801, 126.894778820701, 117
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120137'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '대림역', '7호선', '서울 영등포구 대림동 1050-17', 37.4928430008915, 126.896722858042, 163
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120142'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '대림역', '2호선', '서울 구로구 구로동 1204', 37.4933099444417, 126.894931036051, 263
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120142'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '당산역', '2호선', '서울 영등포구 당산동6가 323-1', 37.5347843171332, 126.902611795523, 130
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120143'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '당산역', '9호선', '서울 영등포구 당산동6가 227-1', 37.5337892432802, 126.902165052778, 141
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120143'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '영등포역', '1호선', '서울 영등포구 영등포동 618-496', 37.5156726288261, 126.907550274975, 348
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120145'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '보라매역', '신림선', '서울 동작구 대방동 466-51', 37.5003072682843, 126.920436608423, 120
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120147'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '보라매역', '7호선', '서울 동작구 대방동 466-51', 37.49992085496506, 126.92061230710924, 75
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120147'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '국회의사당역', '9호선', '서울 영등포구 여의도동 1-6', 37.5281465047706, 126.91783895106, 260
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120148'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '여의도역', '5호선', '서울 영등포구 여의도동 3', 37.5217753947299, 126.924397990207, 192
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120149'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '여의도역', '9호선', '서울 영등포구 여의도동 2-6', 37.5211700890445, 126.92466104293, 219
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120149'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신대방삼거리역', '7호선', '서울 동작구 대방동 406-10', 37.499757228908564, 126.92821094566965, 87
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120152'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '노량진역', '1호선', '서울 동작구 노량진동 67-2', 37.5140547961008, 126.942109882124, 163
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120153'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '노량진역', '9호선', '서울 동작구 노량진동 60-11', 37.5135856714992, 126.940893179777, 72
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120153'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '장승배기역', '7호선', '서울 동작구 상도동 26-20', 37.5048990621814, 126.939072381356, 138
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120154'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '총신대입구역', '4호선', '서울 동작구 사당동 736-1', 37.4867995957995, 126.982211871752, 100
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120156'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신림역', '신림선', '서울 관악구 신림동 1467-10', 37.4848815538296, 126.929629471493, 56
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120157'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신림역', '2호선', '서울 관악구 신림동 1467-10', 37.484267135140364, 126.9297453749671, 67
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120157'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서울대입구역', '2호선', '서울 관악구 봉천동 979-2', 37.4812845080678, 126.952713197762, 80
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120159'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '사당역', '2호선', '서울 동작구 사당동 1129', 37.47656223234824, 126.98155858357366, 116
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120160'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '사당역', '4호선', '서울 동작구 사당동 588-44', 37.4775912070902, 126.98169851997, 34
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120160'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '사당역', '4호선', '서울 동작구 사당동 588-44', 37.4775912070902, 126.98169851997, 415
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120163'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '사당역', '2호선', '서울 동작구 사당동 1129', 37.47656223234824, 126.98155858357366, 491
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120163'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '내방역', '7호선', '서울 서초구 방배동 874-16', 37.4876585212369, 126.993600180589, 59
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120164'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '방배역', '2호선', '서울 서초구 방배동 912-14', 37.4814561268152, 126.997553345516, 34
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120166'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '고속터미널역', '9호선', '서울 서초구 반포동 19-11', 37.5059814899483, 127.004211793489, 139
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120167'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '고속터미널역', '7호선', '서울 서초구 반포동 19-11', 37.5033865633062, 127.005007834609, 187
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120167'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '고속터미널역', '3호선', '서울 서초구 반포동 19-11', 37.50454885557265, 127.00512100872538, 57
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120167'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '서초역', '2호선', '서울 서초구 서초동 1748-5', 37.4918499338918, 127.007662120039, 93
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120169'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '교대역', '3호선', '서울 서초구 서초동 1748-4', 37.4927431676548, 127.013867969161, 134
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120173'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '교대역', '2호선', '서울 서초구 서초동 1748-4', 37.4938999991414, 127.014383829781, 41
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120173'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '논현역', '신분당선', '서울 강남구 논현동 280', 37.5104125196351, 127.021678898276, 444
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120174'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '논현역', '7호선', '서울 강남구 논현동 279-67', 37.51120000205266, 127.02165424259898, 446
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120174'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '남부터미널역', '3호선', '서울 서초구 서초동 1748-30', 37.4851996335293, 127.01620028391, 169
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120175'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '논현역', '신분당선', '서울 강남구 논현동 280', 37.5104125196351, 127.021678898276, 335
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120176'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '논현역', '7호선', '서울 강남구 논현동 279-67', 37.51120000205266, 127.02165424259898, 397
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120176'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신논현역', '신분당선', '서울 강남구 역삼동 858', 37.5036164990854, 127.024926154562, 488
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120177'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신논현역', '9호선', '서울 강남구 역삼동 800', 37.504811111562, 127.025492036104, 500
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120177'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신사역', '신분당선', '서울 강남구 신사동 667', 37.5160820085929, 127.019551734927, 13
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120178'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신사역', '3호선', '서울 강남구 신사동 667', 37.51643597531432, 127.02030856272764, 85
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120178'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '양재역', '3호선', '서울 서초구 서초동 1366-9', 37.48457681195669, 127.03416413380752, 15
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120179'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '양재역', '신분당선', '서울 서초구 서초동 1366-9', 37.4834655780476, 127.035154074503, 151
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120179'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '양재시민의숲역', '신분당선', '서울 서초구 양재동 233-2', 37.4701332061395, 127.038471341381, 187
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120182'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '논현역', '7호선', '서울 강남구 논현동 279-67', 37.51120000205266, 127.02165424259898, 28
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120185'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '논현역', '신분당선', '서울 강남구 논현동 280', 37.5104125196351, 127.021678898276, 73
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120185'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신논현역', '신분당선', '서울 강남구 역삼동 858', 37.5036164990854, 127.024926154562, 160
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120187'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '신논현역', '9호선', '서울 강남구 역삼동 800', 37.504811111562, 127.025492036104, 30
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120187'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '압구정역', '3호선', '서울 강남구 신사동 668', 37.52649127416921, 127.02850865895756, 120
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120188'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '강남역', '2호선', '서울 강남구 역삼동 858', 37.49808633653005, 127.02800140627488, 61
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120189'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '강남역', '신분당선', '서울 강남구 역삼동 858', 37.4967771303817, 127.028185245594, 96
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120189'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '학동역', '7호선', '서울 강남구 논현동 279-67', 37.51434578734927, 127.03190190063152, 183
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120191'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '언주역', '9호선', '서울 강남구 논현동 279-165', 37.5073353959717, 127.033970921595, 9
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120194'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '역삼역', '2호선', '서울 강남구 역삼동 804', 37.5006744185994, 127.03646946847, 55
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120197'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '압구정로데오역', '수인분당선', '서울 강남구 압구정동 495', 37.5275184818021, 127.0406027693898, 93
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120202'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '강남구청역', '7호선', '서울 강남구 삼성동 111-44', 37.51721617197854, 127.0413109462156, 121
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120203'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '강남구청역', '수인분당선', '서울 강남구 삼성동 111-44', 37.5166241449989, 127.04150404599, 83
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120203'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '매봉역', '3호선', '서울 강남구 도곡동 179-2', 37.4869394661317, 127.046711448388, 380
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120205'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '선정릉역', '수인분당선', '서울 강남구 삼성동 111-114', 37.5109326388803, 127.043627289129, 175
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120207'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '선정릉역', '9호선', '서울 강남구 삼성동 111-114', 37.51032431709664, 127.0440148858615, 182
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120207'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '청담역', '7호선', '서울 강남구 청담동 77-76', 37.519455579961, 127.053717937903, 379
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120209'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '선릉역', '2호선', '서울 강남구 삼성동 172-66', 37.504497373023206, 127.04896282498558, 37
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120210'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '선릉역', '수인분당선', '서울 강남구 삼성동 172-66', 37.505167825521674, 127.04870992465413, 84
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120210'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '한티역', '수인분당선', '서울 강남구 대치동 1011-28', 37.4962857047653, 127.052909749417, 141
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120212'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '삼성중앙역', '9호선', '서울 강남구 삼성동 111-147', 37.5129614511319, 127.053043676912, 168
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120213'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '대치역', '3호선', '서울 강남구 대치동 317-3', 37.4944966528582, 127.063203409529, 88
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120220'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '봉은사역', '9호선', '서울 강남구 삼성동 172', 37.5142554489848, 127.060233935114, 192
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120221'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '삼성역', '2호선', '서울 강남구 삼성동 172-66', 37.508822740225305, 127.06302321147605, 21
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120222'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '수서역', 'GTX-A', '서울 강남구 수서동 214-16', 37.4866365875118, 127.102558521485, 134
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120224'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '수서역', '수인분당선', '서울 강남구 수서동 728', 37.48791648624159, 127.10096935722798, 182
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120224'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '수서역', '3호선', '서울 강남구 수서동 728', 37.487459640349336, 127.10205874359781, 93
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120224'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '잠실새내역', '2호선', '서울 송파구 잠실동 33', 37.5116263587296, 127.086314327913, 353
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120225'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '석촌고분역', '9호선', '서울 송파구 삼전동 157-1', 37.50246245994369, 127.0965081858022, 107
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120226'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '잠실역', '2호선', '서울 송파구 신천동 8', 37.51331105877401, 127.10023101886318, 118
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120227'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '잠실역', '8호선', '서울 송파구 신천동 7-4', 37.5143121584933, 127.103270523629, 180
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120227'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '석촌역', '9호선', '서울 송파구 석촌동 209', 37.504972845773686, 127.10676479806769, 119
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120228'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '석촌역', '8호선', '서울 송파구 석촌동 209', 37.5054141216925, 127.107004062699, 79
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120228'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '송파역', '8호선', '서울 송파구 가락동 459-4', 37.4997149535067, 127.112193506363, 228
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120230'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '송파나루역', '9호선', '서울 송파구 방이동 2', 37.51101671275264, 127.11266171422027, 113
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120232'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '몽촌토성역', '8호선', '서울 송파구 신천동 19', 37.5178055794687, 127.11286534895457, 211
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120233'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '가락시장역', '8호선', '서울 송파구 가락동 184-23', 37.4930992522183, 127.118262745146, 182
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120234'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '가락시장역', '3호선', '서울 송파구 가락동 184-23', 37.4922493218664, 127.11764173832, 288
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120234'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '문정역', '8호선', '서울 송파구 문정동 119-4', 37.4860310539381, 127.122484886901, 232
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120235'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '장지역', '8호선', '서울 송파구 장지동 201-5', 37.4786316933099, 127.12617656737, 262
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120236'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '경찰병원역', '3호선', '서울 송파구 가락동 10-15', 37.4960049150853, 127.124482397777, 349
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120237'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '방이역', '5호선', '서울 송파구 방이동 217-2', 37.5087506251989, 127.126096046986, 82
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120238'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '오금역', '5호선', '서울 송파구 오금동 44-2', 37.5021388575469, 127.127953219596, 165
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120239'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '오금역', '3호선', '서울 송파구 오금동 44-2', 37.50232208779385, 127.12848280775154, 215
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120239'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '개롱역', '5호선', '서울 송파구 가락동 165-2', 37.497968278388235, 127.13507959838425, 218
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120240'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '거여역', '5호선', '서울 송파구 거여동 20-14', 37.4933304500736, 127.143773841911, 166
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120241'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '강동구청역', '8호선', '서울 강동구 성내동 319', 37.5302111983522, 127.12044373032, 225
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120242'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '천호역', '5호선', '서울 강동구 천호동 455', 37.5385112120297, 127.12392845044, 121
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120243'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '천호역', '8호선', '서울 강동구 천호동 455', 37.537936231906734, 127.12320335921426, 208
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120243'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '암사역', '8호선', '서울 강동구 암사동 501', 37.55015818096496, 127.12754072968517, 138
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120244'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '둔촌동역', '5호선', '서울 강동구 둔촌동 416', 37.52765684247226, 127.13621464981398, 186
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120245'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '길동역', '5호선', '서울 강동구 길동 378', 37.5378264874159, 127.140021295948, 214
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120246'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '굽은다리역', '5호선', '서울 강동구 명일동 345-12', 37.54551759022203, 127.14288387331297, 39
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120247'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '명일역', '5호선', '서울 강동구 명일동 303-1', 37.5513978958277, 127.14404374474, 34
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120248'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);

INSERT INTO commercial_area_station_lines (
  commercial_area_id, station_name, line_name, station_address, station_latitude, station_longitude, distance_meters
)
SELECT id, '고덕역', '5호선', '서울 강동구 고덕동 310', 37.55504766830918, 127.15416500963447, 196
FROM commercial_areas
WHERE source = 'SEOUL_COMMERCIAL_ANALYSIS' AND external_code = '3120249'
ON DUPLICATE KEY UPDATE
  station_address = VALUES(station_address),
  station_latitude = VALUES(station_latitude),
  station_longitude = VALUES(station_longitude),
  distance_meters = VALUES(distance_meters);
