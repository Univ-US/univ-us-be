package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.lms.code.SecReqStatusCode;
import com.univus.app.lms.dto.LmsStudentProfileResponseDto;
import com.univus.app.lms.dto.LmsStudentProfileUpdateDto;
import com.univus.app.lms.exception.InvalidProfileImageException;
import com.univus.app.lms.mapper.LmsStudentProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LmsStudentProfileServiceImpl implements LmsStudentProfileService {

    private final LmsStudentProfileMapper lmsStudentProfileMapper;
    private final StorageService storageService; // 공통 파일 엔진 재사용

    //TODO: 배포 시 저장 경로 설정 필수
    @Value("${file.upload-root:${user.home}/univus/uploads}") // applicaton.yml settings:업슬 때 경로
    private String uploadRoot;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png"); // JPG/JPEG/PNG
    private static final long MAX_IMAGE_SIZE = 30L * 1024 * 1024; // 30MB
    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_SUBDIR = "lms" + File.separator + "student" + File.separator + "image";
    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_URL_PREFIX = "/uploads/lms/student/image/";

    // SLM-001 조회 (LMS_PROFILE 없으면 지연 생성 후 반환)
    @Override
    @Transactional
    public LmsStudentProfileResponseDto requestGetLmsStudentProfile(Long memberId) {
        ensureLmsProfile(memberId);
        return lmsStudentProfileMapper.findLmsStudentProfileByMemberId(memberId);
    }

    // SLM-001 수정 (이메일, 이미지)
    @Override
    @Transactional
    public LmsStudentProfileResponseDto requestUpdateLmsStudentProfile(Long memberId,
                                                                       LmsStudentProfileUpdateDto dto) {
        Long lmsPrfId = ensureLmsProfile(memberId);

        // 이메일 (LMS_PROFILE)
        lmsStudentProfileMapper.updateLmsStudentProfile(memberId, dto);

        // 이미지 (새 파일이 있을 때만)
        MultipartFile image = dto.getLmsStudentProfileImage();
        if (image != null && !image.isEmpty()) {
            validateImage(image);
            String directoryPath = uploadRoot + File.separator + IMAGE_SUBDIR;
            String trnFileName = storageService.uploadFileToServer(image, directoryPath); // 변환(저장) 파일명
            String orgFileName = image.getOriginalFilename();
            String orgUrl = IMAGE_URL_PREFIX + trnFileName;
            String extType = extractExtension(image.getContentType());

            lmsStudentProfileMapper.invalidateLmsStudentProfileOldImage(lmsPrfId); // 기존 이미지 0 처리
            lmsStudentProfileMapper.insertProfileImage(lmsPrfId, orgFileName, trnFileName, orgUrl, extType); // 신규 insert
        }

        return lmsStudentProfileMapper.findLmsStudentProfileByMemberId(memberId); // 최신 재조회
    }

    // SLM-001/012 탈퇴 요청 (하드삭제 아님 → LMS_USER_SECESSION_REQUEST insert)
    @Override
    @Transactional
    public void requestDeleteLmsStudentProfile(Long memberId) {
        Long lmsPrfId = ensureLmsProfile(memberId);
        // 탈퇴 상태값은 공통코드에서 (SecReqStatusCode.REQ.getCode() = "REQ")
        lmsStudentProfileMapper.insertSecessionRequest(lmsPrfId, SecReqStatusCode.REQ.getCode());
    }

    /* 지연 생성(getOrCreate): LMS_PROFILE 없으면 INSERT 후 lmsPrfId 반환 */
    private Long ensureLmsProfile(Long memberId) {
        Long lmsPrfId = lmsStudentProfileMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            try {
                lmsStudentProfileMapper.insertLmsProfile(memberId);
            } catch (DuplicateKeyException e) {
                // 동시 요청으로 이미 생성됨 → 재조회로 흡수 (MEMBER_ID UNIQUE가 2차 방어)
            }
            lmsPrfId = lmsStudentProfileMapper.findLmsPrfIdByMemberId(memberId);
        }
        return lmsPrfId;
    }

    /*
     * 이미지 검증
     * JPG/PNG ONLY
     * 5MB (화면 기준)
     */
    private void validateImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidProfileImageException("이미지는 JPG/JPEG 또는 PNG 형식만 업로드할 수 있습니다.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidProfileImageException("이미지 용량은 30MB를 초과할 수 없습니다.");
        }
    }

    /* 이미지 타입 추출 ("image/png" → "png") */
    private String extractExtension(String contentType) {
        return contentType.substring(contentType.lastIndexOf("/") + 1);
    }
}
