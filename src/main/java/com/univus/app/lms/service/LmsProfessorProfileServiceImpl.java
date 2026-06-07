package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.lms.code.SecReqStatusCode;
import com.univus.app.lms.dto.LmsProfessorProfileResponseDto;
import com.univus.app.lms.dto.LmsProfessorProfileUpdateDto;
import com.univus.app.lms.exception.InvalidProfileImageException;
import com.univus.app.lms.mapper.LmsProfessorProfileMapper;
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
public class LmsProfessorProfileServiceImpl implements LmsProfessorProfileService {
    private final LmsProfessorProfileMapper lmsProfessorProfileMapper;
    private final StorageService storageService; // common file engine reuse

    // file upload settings
    //TODO: 배포 시 저장 경로 설정 필수
    @Value("${file.upload-root:${user.home}/univus/uploads}") // application.yml setting: 없으면 기본 경로
    private String uploadRoot;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png"); // JPG, JPEG, PNG
    private static final long MAX_IMAGE_SIZE = 30L * 1024 * 1024; // 30MB
    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_SUBDIR = "lms" + File.separator + "professor" + File.separator + "image"; // 저장 하위 폴더
    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_URL_PREFIX = "/uploads/lms/professor/image/"; // 웹 접근 경로

    // PLM-001 교수 프로필 조회 (LMS_PROFILE 없으면 지연 생성 후 반환)
    @Override
    @Transactional // getOrCreate에서 INSERT 가능성이 있어 readOnly 사용 안 함
    public LmsProfessorProfileResponseDto requestGetLmsProfessorProfile(Long memberId) {
        ensureLmsProfile(memberId);
        return lmsProfessorProfileMapper.findLmsprofessorProfileByMemberId(memberId);
    }

    // PLM-001 교수 프로필 수정
    @Override
    @Transactional
    public LmsProfessorProfileResponseDto requestUpdateLmsProfessorProfile(Long memberId,
                                                                           LmsProfessorProfileUpdateDto lmsProfessorProfileUpdateDto) {
        // LMS_PROFILE 없으면 생성 후 lmsPrfId 확보
        Long lmsPrfId = ensureLmsProfile(memberId);
        // 이메일, 소개 수정
        lmsProfessorProfileMapper.updateLmsProfessorProfile(memberId, lmsProfessorProfileUpdateDto);
        // 이미지 처리 (새 이미지가 넘어온 경우에만)
        MultipartFile lmsProfessorProfileImage = lmsProfessorProfileUpdateDto.getLmsProfessorProfileImage();
        if (lmsProfessorProfileImage != null && !lmsProfessorProfileImage.isEmpty()) {
            validateImage(lmsProfessorProfileImage);
            String directoryPath = uploadRoot + File.separator + IMAGE_SUBDIR;
            String trnFileName = storageService.uploadFileToServer(lmsProfessorProfileImage, directoryPath); // 변환(저장) 파일명
            String orgFileName = lmsProfessorProfileImage.getOriginalFilename();
            String orgUrl = IMAGE_URL_PREFIX + trnFileName;
            String extType = extractExtension(lmsProfessorProfileImage.getContentType());

            lmsProfessorProfileMapper.invalidateLmsProfessorProfileOldImage(lmsPrfId); // 기존 이미지 0 처리
            lmsProfessorProfileMapper.insertProfileImage(lmsPrfId, orgFileName, trnFileName, orgUrl, extType); // 신규 이미지 insert
        }
        return lmsProfessorProfileMapper.findLmsprofessorProfileByMemberId(memberId); // 최신 재조회
    }

    // PLM-001/012 탈퇴 요청 (하드삭제 아님 → LMS_USER_SECESSION_REQUEST insert)
    @Override
    @Transactional
    public void requestDeleteLmsProfessorProfile(Long memberId) {
        Long lmsPrfId = ensureLmsProfile(memberId);
        // 탈퇴 상태값은 공통코드에서 (SecReqStatusCode.REQ.getCode() = "REQ")
        lmsProfessorProfileMapper.insertSecessionRequest(lmsPrfId, SecReqStatusCode.REQ.getCode());
    }

    /* 지연 생성(getOrCreate): LMS_PROFILE 없으면 INSERT 후 lmsPrfId 반환 */
    private Long ensureLmsProfile(Long memberId) {
        Long lmsPrfId = lmsProfessorProfileMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            try {
                lmsProfessorProfileMapper.insertLmsProfile(memberId);
            } catch (DuplicateKeyException e) {
                // 동시 요청으로 이미 생성됨 → 재조회로 흡수 (MEMBER_ID UNIQUE가 2차 방어)
            }
            lmsPrfId = lmsProfessorProfileMapper.findLmsPrfIdByMemberId(memberId);
        }
        return lmsPrfId;
    }

    /*
    * 이미지 검증
    * JPG/PNG ONLY
    * 30MB
    * */
    private void validateImage(MultipartFile lmsProfessorProfileImage) {
        String imageContentType = lmsProfessorProfileImage.getContentType();
        // type 검사
        if (imageContentType == null || !ALLOWED_TYPES.contains(imageContentType)) {
            throw new InvalidProfileImageException("이미지는 JPG/JPEG 또는 PNG 형식만 업로드 할 수 있습니다");
        }
        // 30MB 용량 검사
        if (lmsProfessorProfileImage.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidProfileImageException("이미지는 용량은 30MB를 초과할 수 없습니다.");
        }
    }

    /* 이미지 타입 추출 ("image/png" → "png") */
    private String extractExtension(String contentType) {
        return contentType.substring(contentType.lastIndexOf("/") + 1);
    }
}
