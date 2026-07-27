package com.moyeo.service.meeting;

import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Component
public class MeetingCoverProcessor {

    private final MeetingCoverProperties properties;

    public MeetingCoverProcessor(MeetingCoverProperties properties) {
        this.properties = properties;
    }

    public byte[] resizeToJpeg(MultipartFile source) {
        if (source == null || source.isEmpty() || source.getSize() > properties.maxUploadSize().toBytes()) {
            throw new MoyeoException(CommonErrorCode.PAYLOAD_TOO_LARGE);
        }
        if (!"image/jpeg".equals(source.getContentType()) && !"image/png".equals(source.getContentType())) {
            throw new MoyeoException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        try (InputStream sourceInput = source.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(sourceInput)) {
            if (imageInput == null) {
                throw new MoyeoException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new MoyeoException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }
            ImageReader reader = readers.next();
            try {
                if (!isSupportedImageFormat(reader.getFormatName())) {
                    throw new MoyeoException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
                }
                reader.setInput(imageInput, true, true);
                int sourceWidth = reader.getWidth(0);
                int sourceHeight = reader.getHeight(0);
                validateSourceDimensions(sourceWidth, sourceHeight);
                return resize(reader.read(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new MoyeoException(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private boolean isSupportedImageFormat(String formatName) {
        return "JPEG".equalsIgnoreCase(formatName) || "PNG".equalsIgnoreCase(formatName);
    }

    private void validateSourceDimensions(int width, int height) {
        long pixels = (long) width * height;
        if (width <= 0
                || height <= 0
                || width > properties.maxSourceWidth()
                || height > properties.maxSourceHeight()
                || pixels > properties.maxSourcePixels()) {
            throw new MoyeoException(CommonErrorCode.PAYLOAD_TOO_LARGE);
        }
    }

    private byte[] resize(BufferedImage input) throws IOException {
        int[] size = constrainedSize(input.getWidth(), input.getHeight());
        BufferedImage output = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, size[0], size[1]);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(input, 0, 0, size[0], size[1], null);
        } finally {
            graphics.dispose();
        }
        return writeJpeg(output);
    }

    private int[] constrainedSize(int width, int height) {
        double scale = Math.min(1.0, Math.min((double) properties.maxWidth() / width, (double) properties.maxHeight() / height));
        return new int[]{Math.max(1, (int) Math.round(width * scale)), Math.max(1, (int) Math.round(height * scale))};
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(properties.jpegQuality());
            writer.write(null, new IIOImage(image, null, null), parameters);
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
