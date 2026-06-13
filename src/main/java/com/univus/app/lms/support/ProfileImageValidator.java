package com.univus.app.lms.support;

import com.univus.app.lms.exception.InvalidProfileImageException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * LMS 프로필 이미지(교수 PLM-001 / 학생 SLM-001 공용) 업로드 검증.
 * - 형식: JPG/JPEG/PNG 만 허용 (Content-Type)
 * - 용량: 30MB 이하
 * - 시그니처(매직바이트): 파일 선두 바이트가 실제 JPEG/PNG 인지 확인 → Content-Type 위조(MIME 스푸핑) 차단
 * 위반 시 {@link InvalidProfileImageException}.
 */
public final class ProfileImageValidator {

    private ProfileImageValidator() {
    }

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_IMAGE_SIZE = 30L * 1024 * 1024; // 30MB

    public static void validate(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidProfileImageException("이미지는 JPG/JPEG 또는 PNG 형식만 업로드할 수 있습니다.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidProfileImageException("이미지 용량은 30MB를 초과할 수 없습니다.");
        }
        if (!hasAllowedImageSignature(image)) {
            throw new InvalidProfileImageException("이미지 파일이 손상되었거나 JPG/PNG 형식이 아닙니다.");
        }
    }

    /* 파일 선두 8바이트를 읽어 JPEG/PNG 시그니처인지 확인 (확장자·Content-Type이 아닌 실제 내용 기준).
       getInputStream()은 호출마다 새 스트림을 열어주므로 이후 업로드 저장에 영향을 주지 않는다. */
    private static boolean hasAllowedImageSignature(MultipartFile image) {
        byte[] header = new byte[8];
        int read;
        try (InputStream in = image.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw new InvalidProfileImageException("이미지 파일을 읽을 수 없습니다.");
        }
        return isJpeg(header, read) || isPng(header, read);
    }

    /* JPEG 시그니처: FF D8 FF */
    private static boolean isJpeg(byte[] h, int read) {
        return read >= 3
                && (h[0] & 0xFF) == 0xFF
                && (h[1] & 0xFF) == 0xD8
                && (h[2] & 0xFF) == 0xFF;
    }

    /* PNG 시그니처: 89 50 4E 47 0D 0A 1A 0A */
    private static boolean isPng(byte[] h, int read) {
        return read >= 8
                && (h[0] & 0xFF) == 0x89
                && (h[1] & 0xFF) == 0x50
                && (h[2] & 0xFF) == 0x4E
                && (h[3] & 0xFF) == 0x47
                && (h[4] & 0xFF) == 0x0D
                && (h[5] & 0xFF) == 0x0A
                && (h[6] & 0xFF) == 0x1A
                && (h[7] & 0xFF) == 0x0A;
    }
}
