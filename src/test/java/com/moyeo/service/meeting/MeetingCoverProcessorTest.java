package com.moyeo.service.meeting;

import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingCoverProcessorTest {

    private final MeetingCoverProcessor processor = new MeetingCoverProcessor(
            new MeetingCoverProperties(
                    "bucket",
                    "ap-northeast-2",
                    DataSize.ofMegabytes(10),
                    1280,
                    720,
                    8000,
                    8000,
                    13_000_000,
                    0.85f
            )
    );

    @Test
    void validImageIsDecodedAndResizedWithinOutputBounds() throws Exception {
        MockMultipartFile source = pngFile(pngImage(1600, 900));

        byte[] resized = processor.resizeToJpeg(source);
        BufferedImage output = ImageIO.read(new ByteArrayInputStream(resized));

        assertThat(output.getWidth()).isEqualTo(1280);
        assertThat(output.getHeight()).isEqualTo(720);
    }

    @Test
    void commonTwelveMegapixelImageIsAccepted() throws Exception {
        MockMultipartFile source = pngFile(pngImage(4032, 3024));

        byte[] resized = processor.resizeToJpeg(source);
        BufferedImage output = ImageIO.read(new ByteArrayInputStream(resized));

        assertThat(output.getWidth()).isEqualTo(960);
        assertThat(output.getHeight()).isEqualTo(720);
    }

    @Test
    void excessivePixelCountIsRejectedFromHeaderBeforeImageDecode() throws Exception {
        byte[] oversizedHeader = withPngDimensions(pngImage(1, 1), 5000, 5000);

        assertPayloadTooLarge(pngFile(oversizedHeader));
    }

    @Test
    void excessiveSourceDimensionIsRejectedFromHeaderBeforeImageDecode() throws Exception {
        byte[] oversizedHeader = withPngDimensions(pngImage(1, 1), 9000, 1000);

        assertPayloadTooLarge(pngFile(oversizedHeader));
    }

    @Test
    void encodedFileOverTenMegabytesIsRejectedBeforeImageDecode() {
        byte[] oversizedFile = new byte[(10 * 1024 * 1024) + 1];

        assertPayloadTooLarge(pngFile(oversizedFile));
    }

    @Test
    void encodedFileAtTenMegabytesIsAccepted() throws Exception {
        byte[] validPng = pngImage(1, 1);
        byte[] exactLimit = Arrays.copyOf(validPng, 10 * 1024 * 1024);

        byte[] resized = processor.resizeToJpeg(pngFile(exactLimit));

        assertThat(ImageIO.read(new ByteArrayInputStream(resized))).isNotNull();
    }

    @Test
    void nonJpegOrPngContentIsRejectedEvenWhenTheClientClaimsPng() throws Exception {
        MockMultipartFile disguisedGif = pngFile(gifImage(1, 1));

        assertThatThrownBy(() -> processor.resizeToJpeg(disguisedGif))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE)
                );
    }

    @Test
    void twoNearLimitImagesCanBeProcessedConcurrently() throws Exception {
        byte[] nearLimitImage = pngImage(4032, 3024);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(
                    executor.submit(() -> resizeAfterSignal(nearLimitImage, ready, start)),
                    executor.submit(() -> resizeAfterSignal(nearLimitImage, ready, start))
            );
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (var task : tasks) {
                byte[] resized = task.get(30, TimeUnit.SECONDS);
                BufferedImage output = ImageIO.read(new ByteArrayInputStream(resized));
                assertThat(output.getWidth()).isEqualTo(960);
                assertThat(output.getHeight()).isEqualTo(720);
            }
        }
    }

    private byte[] resizeAfterSignal(
            byte[] image,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent image-processing start timed out.");
        }
        return processor.resizeToJpeg(pngFile(image));
    }

    private void assertPayloadTooLarge(MockMultipartFile source) {
        assertThatThrownBy(() -> processor.resizeToJpeg(source))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.PAYLOAD_TOO_LARGE)
                );
    }

    private MockMultipartFile pngFile(byte[] content) {
        return new MockMultipartFile("coverImage", "cover.png", "image/png", content);
    }

    private byte[] pngImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private byte[] gifImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "gif", output);
        return output.toByteArray();
    }

    private byte[] withPngDimensions(byte[] png, int width, int height) {
        byte[] modified = png.clone();
        ByteBuffer buffer = ByteBuffer.wrap(modified);
        buffer.putInt(16, width);
        buffer.putInt(20, height);

        CRC32 crc = new CRC32();
        crc.update(modified, 12, 17);
        buffer.putInt(29, (int) crc.getValue());
        return modified;
    }
}
