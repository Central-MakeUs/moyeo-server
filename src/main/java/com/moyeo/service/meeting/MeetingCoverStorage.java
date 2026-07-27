package com.moyeo.service.meeting;

import java.time.Instant;
import java.util.List;

public interface MeetingCoverStorage {

    void put(String objectKey, byte[] content);

    CoverObject get(String objectKey);

    void delete(String objectKey);

    List<StoredObject> list(String prefix);

    record CoverObject(byte[] content, String contentType) {
    }

    record StoredObject(String objectKey, Instant lastModifiedAt) {
    }
}
