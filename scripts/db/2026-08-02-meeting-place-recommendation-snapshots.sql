CREATE TABLE meeting_place_recommendation_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    `rank` INT NOT NULL,
    area_code VARCHAR(30) NOT NULL,
    area_name VARCHAR(255) NOT NULL,
    category_name VARCHAR(30) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    gu_name VARCHAR(40) NULL,
    dong_name VARCHAR(40) NULL,
    average_straight_distance_meters BIGINT NULL,
    average_travel_time_seconds BIGINT NOT NULL,
    max_travel_time_seconds BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meeting_place_recommendation_snapshots_meeting_rank UNIQUE (meeting_id, `rank`),
    CONSTRAINT fk_meeting_place_recommendation_snapshots_meeting
        FOREIGN KEY (meeting_id) REFERENCES meetings (id)
);
