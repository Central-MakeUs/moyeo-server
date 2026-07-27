-- Apply once before deploying persistent commercial-area recommendations to an existing production database.
-- Back up the database and run 2026-07-27-commercial-areas-seoul.sql immediately after this script.
CREATE TABLE commercial_areas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(40) NOT NULL COMMENT '상권 데이터 출처',
    external_code VARCHAR(30) NOT NULL COMMENT '출처가 부여한 상권 코드',
    area_type VARCHAR(30) NOT NULL COMMENT '추천 대상 상권 유형',
    area_name VARCHAR(255) NOT NULL COMMENT '상권명',
    latitude DECIMAL(10, 7) NOT NULL COMMENT '상권 중심 WGS84 위도',
    longitude DECIMAL(10, 7) NOT NULL COMMENT '상권 중심 WGS84 경도',
    district_code VARCHAR(10) NULL COMMENT '시군구 코드',
    district_name VARCHAR(40) NULL COMMENT '시군구명',
    administrative_dong_code VARCHAR(12) NULL COMMENT '행정동 코드',
    administrative_dong_name VARCHAR(40) NULL COMMENT '행정동명',
    PRIMARY KEY (id),
    CONSTRAINT uk_commercial_areas_source_external_code UNIQUE (source, external_code),
    INDEX idx_commercial_areas_source_type (source, area_type)
) COMMENT='추천 후보 상권 데이터';
