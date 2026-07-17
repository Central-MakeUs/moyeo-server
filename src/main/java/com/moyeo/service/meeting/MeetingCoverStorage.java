package com.moyeo.service.meeting;

public interface MeetingCoverStorage {

    void put(String objectKey, byte[] content);

    CoverObject get(String objectKey);

    void delete(String objectKey);

    record CoverObject(byte[] content, String contentType) {
    }
}
