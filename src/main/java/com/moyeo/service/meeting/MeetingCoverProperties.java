package com.moyeo.service.meeting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "moyeo.meeting-cover")
public record MeetingCoverProperties(
        String bucket,
        String region,
        DataSize maxUploadSize,
        int maxWidth,
        int maxHeight,
        float jpegQuality
) {
}
