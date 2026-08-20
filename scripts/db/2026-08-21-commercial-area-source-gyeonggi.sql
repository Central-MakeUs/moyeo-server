-- Apply before 2026-08-21-commercial-areas-gyeonggi.sql when the existing
-- commercial_areas.source column was created by Hibernate as a native MySQL ENUM.
ALTER TABLE commercial_areas
    MODIFY COLUMN source ENUM(
        'SEOUL_COMMERCIAL_ANALYSIS',
        'GYEONGGI_DEVELOPMENT_COMMERCIAL'
    ) NOT NULL COMMENT '상권 데이터 출처';
