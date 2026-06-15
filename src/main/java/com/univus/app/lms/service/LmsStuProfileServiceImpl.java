package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.commoncode.code.RoleCode;
import com.univus.app.lms.dto.LmsStuProfileDto;
import com.univus.app.lms.mapper.LmsStuProfileMapper;
import com.univus.app.lms.support.LmsProfileImageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
@RequiredArgsConstructor
public class LmsStuProfileServiceImpl implements LmsStuProfileService {

    private final LmsStuProfileMapper lmsStuProfileMapper;
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
    public LmsStuProfileDto.ResDto requestGetLmsStudentProfile(Long memberId) {
        ensureLmsProfile(memberId);
        return findWithRoleLabel(memberId);
    }

    // SLM-001 수정 (이메일, 이미지)
    @Override
    @Transactional
    public LmsStuProfileDto.ResDto requestUpdateLmsStudentProfile(Long memberId,
                                                                  LmsStuProfileDto.ReqDto dto) {
        Long lmsPrfId = ensureLmsProfile(memberId);

        // 이메일 (LMS_PROFILE)
        lmsStuProfileMapper.updateLmsStudentProfile(memberId, dto);

        // 이미지 (새 파일이 있을 때만)
        MultipartFile image = dto.getLmsStudentProfileImage();
        if (image != null && !image.isEmpty()) {
            LmsProfileImageValidator.validate(image); // 형식·용량·매직바이트 검증(공용)
            String directoryPath = uploadRoot + File.separator + IMAGE_SUBDIR;
            String trnFileName = storageService.uploadFileToServer(image, directoryPath); // 변환(저장) 파일명
            String orgFileName = image.getOriginalFilename();
            String orgUrl = IMAGE_URL_PREFIX + trnFileName;
            String extType = extractExtension(image.getContentType());

            lmsStuProfileMapper.invalidateLmsStudentProfileOldImage(lmsPrfId); // 기존 이미지 0 처리
            lmsStuProfileMapper.insertProfileImage(lmsPrfId, orgFileName, trnFileName, orgUrl, extType); // 신규 insert
        }

        return findWithRoleLabel(memberId); // 최신 재조회
    }

    /* 매핑 DTO 조회 → ResDto 변환 (역할 코드→한글 라벨, 학번은 매퍼가 LOGIN_ID로 내려줌) */
    private LmsStuProfileDto.ResDto findWithRoleLabel(Long memberId) {
        LmsStuProfileDto m = lmsStuProfileMapper.findLmsStudentProfileByMemberId(memberId);
        if (m == null) {
            return null;
        }
        return LmsStuProfileDto.ResDto.builder()
                .lmsStudentProfileName(m.getName())
                .lmsStudentProfileStudentNo(m.getStudentNo())
                .lmsStudentProfileDepartment(m.getDepartment())
                .lmsStudentProfilePhoneNumber(m.getPhoneNumber())
                .lmsStudentProfileEmail(m.getEmail())
                .lmsStudentProfileImageUrl(m.getImageUrl())
                .lmsStudentProfileUniversityName(m.getUniversityName())
                .lmsStudentProfileRole(toRoleLabel(m.getRole()))
                .build();
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
        Long lmsPrfId = lmsStuProfileMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            try {
                lmsStuProfileMapper.insertLmsProfile(memberId);
            } catch (DuplicateKeyException e) {
                // 동시 요청으로 이미 생성됨 → 재조회로 흡수 (MEMBER_ID UNIQUE가 2차 방어)
            }
            lmsPrfId = lmsStuProfileMapper.findLmsPrfIdByMemberId(memberId);
        }
        return lmsPrfId;
    }

    /* 이미지 타입 추출 ("image/png" → "png") */
    private String extractExtension(String contentType) {
        return contentType.substring(contentType.lastIndexOf("/") + 1);
    }
}
