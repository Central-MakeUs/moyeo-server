package com.moyeo.service.meeting;

import com.moyeo.global.error.MoyeoException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3MeetingCoverStorageTest {
    private MeetingCoverProperties properties(Duration total, Duration attempt) {
        return new MeetingCoverProperties("test-bucket", "us-east-1", DataSize.ofMegabytes(10),
                1280, 720, 8000, 8000, 13_000_000, 0.85f, total, attempt);
    }

    @Test
    void missingObjectIs404ButOtherServiceAndNetworkErrorsRemain503() {
        S3Client client = mock(S3Client.class);
        var storage = new S3MeetingCoverStorage(properties(Duration.ofSeconds(5), Duration.ofSeconds(3)), client);
        doThrow(NoSuchKeyException.builder().statusCode(404).message("missing").build())
                .when(client).getObjectAsBytes(any(GetObjectRequest.class));
        assertThatThrownBy(() -> storage.get("missing.jpg")).isInstanceOfSatisfying(MoyeoException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(MeetingCoverErrorCode.MEETING_COVER_IMAGE_NOT_FOUND));
        doThrow(S3Exception.builder().statusCode(403).message("forbidden").build())
                .when(client).getObjectAsBytes(any(GetObjectRequest.class));
        assertThatThrownBy(() -> storage.get("private.jpg")).isInstanceOfSatisfying(MoyeoException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(MeetingCoverErrorCode.MEETING_COVER_IMAGE_UNAVAILABLE));
        doThrow(SdkClientException.create("network failed"))
                .when(client).getObjectAsBytes(any(GetObjectRequest.class));
        assertThatThrownBy(() -> storage.get("offline.jpg")).isInstanceOfSatisfying(MoyeoException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(MeetingCoverErrorCode.MEETING_COVER_IMAGE_UNAVAILABLE));
    }

    @Test
    void slowS3ResponseIsBoundedAndMappedTo503() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/", exchange -> {
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try (S3Client client = S3Client.builder().region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .forcePathStyle(true)
                .overrideConfiguration(S3MeetingCoverStorage.clientOverride(properties(Duration.ofMillis(500), Duration.ofMillis(300))))
                .build()) {
            var storage = new S3MeetingCoverStorage(properties(Duration.ofMillis(500), Duration.ofMillis(300)), client);
            long started = System.nanoTime();
            assertThatThrownBy(() -> storage.get("slow.jpg")).isInstanceOfSatisfying(MoyeoException.class,
                    exception -> assertThat(exception.getErrorCode()).isEqualTo(MeetingCoverErrorCode.MEETING_COVER_IMAGE_UNAVAILABLE));
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }
}
