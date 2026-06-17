package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.commoncode.code.RoleCode;
import com.univus.app.lms.dto.LmsProfProfileDto;
import com.univus.app.lms.mapper.LmsProfProfileMapper;
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
public class LmsProfProfileServiceImpl implements LmsProfProfileService {
    private final LmsProfProfileMapper lmsProfProfileMapper;
    private final StorageService storageService; // common file engine reuse

    // file upload settings
    //TODO: 배포 시 저장 경로 설정 필수
    @Value("${file.upload-root:${user.home}/univus/uploads}") // application.yml setting: 없으면 기본 경로
    private String uploadRoot;
    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_SUBDIR = "lms" + File.separator + "professor" + File.separator + "image"; // 저장 하위 폴더
    //TODO: 배포 시 저장 경로 설정 필수
    private static final String IMAGE_URL_PREFIX = "/uploads/lms/professor/image/"; // 웹 접근 경로

    // PLM-001 교수 프로필 조회 (LMS_PROFILE 없으면 지연 생성 후 반환)
    @Override
    @Transactional // getOrCreate에서 INSERT 가능성이 있어 readOnly 사용 안 함
    public LmsProfProfileDto.ResDto requestGetLmsProfessorProfile(Long memberId) {
        ensureLmsProfile(memberId);
        return findProfileWithRoleLabel(memberId);
    }

    // PLM-001 교수 프로필 수정
    @Override
    @Transactional
    public LmsProfProfileDto.ResDto requestUpdateLmsProfessorProfile(Long memberId,
                                                                     LmsProfProfileDto.ReqDto lmsProfessorProfileUpdateDto) {
        // LMS_PROFILE 없으면 생성 후 lmsPrfId 확보
        Long lmsPrfId = ensureLmsProfile(memberId);
        // 이메일, 소개 수정
        lmsProfProfileMapper.updateLmsProfessorProfile(memberId, lmsProfessorProfileUpdateDto);
        // 이미지 처리 (새 이미지가 넘어온 경우에만)
        MultipartFile lmsProfessorProfileImage = lmsProfessorProfileUpdateDto.getImage();
        if (lmsProfessorProfileImage != null && !lmsProfessorProfileImage.isEmpty()) {
            LmsProfileImageValidator.validate(lmsProfessorProfileImage); // 형식·용량·매직바이트 검증(공용)
            String directoryPath = uploadRoot + File.separator + IMAGE_SUBDIR;
            String trnFileName = storageService.uploadFileToServer(lmsProfessorProfileImage, directoryPath); // 변환(저장) 파일명
            String orgFileName = lmsProfessorProfileImage.getOriginalFilename();
            String orgUrl = IMAGE_URL_PREFIX + trnFileName;
            String extType = extractExtension(lmsProfessorProfileImage.getContentType());

            lmsProfProfileMapper.invalidateLmsProfessorProfileOldImage(lmsPrfId); // 기존 이미지 0 처리
            lmsProfProfileMapper.insertProfileImage(lmsPrfId, orgFileName, trnFileName, orgUrl, extType); // 신규 이미지 insert
        }
        return findProfileWithRoleLabel(memberId); // 최신 재조회
    }

    /* 매핑 DTO 조회 → ResDto 변환 (역할 코드→한글 라벨, get/update 공통) */
    private LmsProfProfileDto.ResDto findProfileWithRoleLabel(Long memberId) {
        LmsProfProfileDto m = lmsProfProfileMapper.findLmsprofessorProfileByMemberId(memberId);
        if (m == null) {
            return null;
        }
        return LmsProfProfileDto.ResDto.builder()
                .name(m.getName())
                .employeeNo(m.getEmployeeNo())
                .department(m.getDepartment())
                .phoneNumber(m.getPhoneNumber())
                .lmsPrfEmail(m.getLmsPrfEmail())
                .lmsPrfIntro(m.getLmsPrfIntro())
                .imageUrl(m.getImageUrl())
                .universityName(m.getUniversityName())
                .role(toRoleLabel(m.getRole()))
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
        Long lmsPrfId = lmsProfProfileMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            try {
                lmsProfProfileMapper.insertLmsProfile(memberId);
            } catch (DuplicateKeyException e) {
                // 동시 요청으로 이미 생성됨 → 재조회로 흡수 (MEMBER_ID UNIQUE가 2차 방어)
            }
            lmsPrfId = lmsProfProfileMapper.findLmsPrfIdByMemberId(memberId);
        }
        return lmsPrfId;
    }

    /* 이미지 타입 추출 ("image/png" → "png") */
    private String extractExtension(String contentType) {
        return contentType.substring(contentType.lastIndexOf("/") + 1);
    }
}
