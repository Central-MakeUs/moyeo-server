-- Apply after 2026-07-27-commercial-areas.sql and its Seoul seed script.
CREATE TABLE commercial_area_station_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    commercial_area_id BIGINT NOT NULL COMMENT '추천 상권 내부 ID',
    station_name VARCHAR(100) NOT NULL COMMENT '카카오 검증 역명',
    line_name VARCHAR(100) NOT NULL COMMENT '카카오 검증 호선명',
    station_address VARCHAR(255) NOT NULL COMMENT '카카오 검증 역 주소',
    station_latitude DECIMAL(18, 15) NOT NULL COMMENT '카카오 검증 역 WGS84 위도',
    station_longitude DECIMAL(18, 15) NOT NULL COMMENT '카카오 검증 역 WGS84 경도',
    distance_meters INT NOT NULL COMMENT '상권 중심과 역 좌표의 직선거리 미터',
    PRIMARY KEY (id),
    CONSTRAINT uk_commercial_area_station_lines_area_station_line
        UNIQUE (commercial_area_id, station_name, line_name),
    CONSTRAINT fk_commercial_area_station_lines_area
        FOREIGN KEY (commercial_area_id) REFERENCES commercial_areas (id)
) COMMENT='추천 상권별 지하철역·호선 매핑';
