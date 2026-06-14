package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuCalendarDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SLM-010 학생 캘린더 Mapper (학생 전용) — 수강 강의의 시간표 + 과제 마감 */
@Mapper
public interface LmsStuCalendarMapper {

    /** 학생 본인 LMS_PRF_ID 조회 (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 수강 강의(LECTURE_STUDENT_ENROLLMENT, status!='DRP')의 주간 강의 시간표 — 학기가 from~to와 겹치는 비폐강 강의만 (날짜 전개는 service) */
    List<LmsStuCalendarDto.LectureTimeRow> selectLectureTimes(@Param("lmsPrfId") Long lmsPrfId,
                                                              @Param("from") String from,
                                                              @Param("to") String to);

    /** 수강 강의의 과제 마감 — from~to 범위 내, 비폐강 강의만 */
    List<LmsStuCalendarDto.AssignmentDueRow> selectAssignmentDues(@Param("lmsPrfId") Long lmsPrfId,
                                                                 @Param("from") String from,
                                                                 @Param("to") String to);
}
