package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuProfileDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LmsStuProfileMapper {

    // 조회 (MEMBER + MEMBER_DETAIL + DEPARTMENT + LMS_PROFILE + 최신 유효 이미지)
    LmsStuProfileDto findLmsStudentProfileByMemberId(@Param("memberId") Long memberId);

    // LMS_PRF_ID 조회 (없으면 null)
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    // 지연 생성: LMS_PROFILE INSERT
    void insertLmsProfile(@Param("memberId") Long memberId);

    // 이메일 수정 (LMS_PROFILE)
    void updateLmsStudentProfile(@Param("memberId") Long memberId,
                                 @Param("dto") LmsStuProfileDto.ReqDto dto);

    // 기존 유효 이미지 무효화 (1 → 0)
    void invalidateLmsStudentProfileOldImage(@Param("lmsPrfId") Long lmsPrfId);

    // 새 이미지 insert (SEQ_LMS_PROFILE_IMAGE)
    void insertProfileImage(@Param("lmsPrfId") Long lmsPrfId,
                            @Param("orgFileName") String orgFileName,
                            @Param("trnFileName") String trnFileName,
                            @Param("orgUrl") String orgUrl,
                            @Param("extType") String extType);
}
