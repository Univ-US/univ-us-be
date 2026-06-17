package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfCoursesDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** PLM-002 교수 "강의 내역" 매퍼 */
@Mapper
public interface LmsProfCoursesMapper {

    /** 교수 본인 LMS_PRF_ID (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 교수 담당 강의 전체 (학기 정보 + 집계값 flat, 학기 시작일 최신순) */
    List<LmsProfCoursesDto.CourseFlatRow> selectCoursesFlat(@Param("lmsPrfId") Long lmsPrfId);
}
