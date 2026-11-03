package com.moyeo.service.meeting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import java.time.Duration;

@ConfigurationProperties(prefix = "moyeo.meeting-cover")
public record MeetingCoverProperties(
        String bucket,
        String region,
        DataSize maxUploadSize,
        int maxWidth,
        int maxHeight,
        int maxSourceWidth,
        int maxSourceHeight,
        long maxSourcePixels,
        float jpegQuality,
        Duration apiCallTimeout,
        Duration apiCallAttemptTimeout
) {
    public MeetingCoverProperties {
        if (apiCallTimeout == null || apiCallTimeout.isZero() || apiCallTimeout.isNegative()
                || apiCallAttemptTimeout == null || apiCallAttemptTimeout.isZero() || apiCallAttemptTimeout.isNegative()
                || apiCallAttemptTimeout.compareTo(apiCallTimeout) > 0) {
            throw new IllegalArgumentException("Cover S3 timeouts must be positive; attempt timeout must not exceed call timeout.");
        }
    }
}
