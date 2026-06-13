package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.commoncode.code.RoleCode;
import com.univus.app.lms.code.LmsUsrSecReqStatusCode;
import com.univus.app.lms.dto.LmsStudentProfileResponseDto;
import com.univus.app.lms.dto.LmsStudentProfileUpdateDto;
import com.univus.app.lms.mapper.LmsStudentProfileMapper;
import com.univus.app.lms.support.ProfileImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
@RequiredArgsConstructor
public class LmsStudentProfileServiceImpl implements LmsStudentProfileService {

    private final LmsStudentProfileMapper lmsStudentProfileMapper;
    private final StorageService storageService; // 공통 파일 엔진 재사용

    //TODO: 배포 시 저장 경로 설정 필수
    @Value("${file.upload-root:${user.home}/univus/uploads}") // applicaton.yml settings:업슬 때 경로
    private String uploadRoot;

    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_SUBDIR = "lms" + File.separator + "student" + File.separator + "image";
    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_URL_PREFIX = "/uploads/lms/student/image/";

    // SLM-001 조회 (LMS_PROFILE 없으면 지연 생성 후 반환)
    @Override
    @Transactional
    public LmsStudentProfileResponseDto requestGetLmsStudentProfile(Long memberId) {
        ensureLmsProfile(memberId);
        return findWithRoleLabel(memberId);
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
            ProfileImageValidator.validate(image); // 형식·용량·매직바이트 검증(공용)
            String directoryPath = uploadRoot + File.separator + IMAGE_SUBDIR;
            String trnFileName = storageService.uploadFileToServer(image, directoryPath); // 변환(저장) 파일명
            String orgFileName = image.getOriginalFilename();
            String orgUrl = IMAGE_URL_PREFIX + trnFileName;
            String extType = extractExtension(image.getContentType());

            lmsStudentProfileMapper.invalidateLmsStudentProfileOldImage(lmsPrfId); // 기존 이미지 0 처리
            lmsStudentProfileMapper.insertProfileImage(lmsPrfId, orgFileName, trnFileName, orgUrl, extType); // 신규 insert
        }

        return findWithRoleLabel(memberId); // 최신 재조회
    }

    // SLM-001/012 탈퇴 요청 (하드삭제 아님 → LMS_USER_SECESSION_REQUEST insert)
    @Override
    @Transactional
    public void requestDeleteLmsStudentProfile(Long memberId) {
        Long lmsPrfId = ensureLmsProfile(memberId);
        // 탈퇴 요청 상태값 = "REQ" (공통코드 LMS_USR_SEC_REQ_STATUS)
        lmsStudentProfileMapper.insertSecessionRequest(lmsPrfId, LmsUsrSecReqStatusCode.REQUESTED.getCode());
    }

    /* 조회 + 역할 코드→한글 라벨 변환 (학번은 매퍼가 LOGIN_ID로 바로 내려줌) */
    private LmsStudentProfileResponseDto findWithRoleLabel(Long memberId) {
        LmsStudentProfileResponseDto dto = lmsStudentProfileMapper.findLmsStudentProfileByMemberId(memberId);
        if (dto != null) {
            dto.setLmsStudentProfileRole(toRoleLabel(dto.getLmsStudentProfileRole())); // 역할 코드→라벨
        }
        return dto;
    }

    /* MEMBER.ROLE 코드 → RoleCode 한글 라벨 (미정의/널 코드는 원본 유지) */
    private String toRoleLabel(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        try {
            return RoleCode.fromCode(roleCode).getLabel();
        } catch (IllegalArgumentException e) {
            return roleCode;
        }
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

    /* 이미지 타입 추출 ("image/png" → "png") */
    private String extractExtension(String contentType) {
        return contentType.substring(contentType.lastIndexOf("/") + 1);
    }
}
