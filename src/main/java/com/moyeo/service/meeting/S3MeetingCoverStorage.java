package com.moyeo.service.meeting;

import com.moyeo.global.error.MoyeoException;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3MeetingCoverStorage implements MeetingCoverStorage {

    private final MeetingCoverProperties properties;
    private final S3Client s3Client;

    public S3MeetingCoverStorage(MeetingCoverProperties properties) {
        this.properties = properties;
        this.s3Client = S3Client.builder().region(Region.of(properties.region())).build();
    }

    @Override
    public void put(String objectKey, byte[] content) {
        try {
            s3Client.putObject(PutObjectRequest.builder().bucket(bucket()).key(objectKey)
                    .contentType("image/jpeg").cacheControl("public, max-age=31536000, immutable").build(),
                    RequestBody.fromBytes(content));
        } catch (S3Exception | SdkClientException exception) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_UNAVAILABLE);
        }
    }

    @Override
    public CoverObject get(String objectKey) {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket()).key(objectKey).build());
            return new CoverObject(response.asByteArray(), response.response().contentType());
        } catch (NoSuchKeyException exception) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_NOT_FOUND);
        } catch (S3Exception | SdkClientException exception) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_UNAVAILABLE);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(builder -> builder.bucket(bucket()).key(objectKey));
        } catch (S3Exception | SdkClientException exception) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_UNAVAILABLE);
        }
    }

    private String bucket() {
        if (properties.bucket() == null || properties.bucket().isBlank()) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_UNAVAILABLE);
        }
        return properties.bucket();
    }
}
