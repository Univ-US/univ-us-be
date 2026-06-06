package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfessorProfileResponseDto;
import com.univus.app.lms.dto.LmsProfessorProfileUpdateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LmsProfessorProfileMapper {
    // 조회 (MEMBER + MEMBER_DETAIL + DEPARTMENT + LMS_PROFILE + 최신 유효 이미지)
    LmsProfessorProfileResponseDto findLmsprofessorProfileByMemberId(@Param("memberId") Long memberId);

    /* LMS_PRF_ID 조회 (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /* 지연 생성: LMS_PROFILE INSERT (PK는 SEQ_LMS_PROFILE 시퀀스) */
    void insertLmsProfile(@Param("memberId") Long memberId);

    /* 이메일, 소개 수정 (LMS_PROFILE) */
    void updateLmsProfessorProfile(@Param("memberId") Long memberId,
                                   @Param("dto") LmsProfessorProfileUpdateDto lmsProfessorProfileUpdateDto);

    /* 기존 이미지 상태 무효화 (1 → 0) */
    void invalidateLmsProfessorProfileOldImage(@Param("lmsProfessorProfileId") Long lmsProfessorProfileId);

    /* 새 이미지 insert */
    void insertProfileImage(@Param("lmsPrfId") Long lmsPrfId,
                           @Param("orgFileName") String orgFileName,
                           @Param("trnFileName") String trnFileName,
                           @Param("orgUrl") String orgUrl,
                           @Param("extType") String extType);

    /* 탈퇴 요청 insert (LMS_USER_SECESSION_REQUEST). status는 공통코드 SecReqStatusCode에서 전달받음 */
    void insertSecessionRequest(@Param("lmsPrfId") Long lmsPrfId,
                                @Param("status") String status);
}
