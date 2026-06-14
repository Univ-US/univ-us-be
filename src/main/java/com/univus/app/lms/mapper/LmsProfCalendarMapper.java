package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfCalendarDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** PLM-010 교수 캘린더 Mapper (교수 전용) — 담당 강의의 시간표 + 과제 마감 */
@Mapper
public interface LmsProfCalendarMapper {

    /** 교수 본인 LMS_PRF_ID 조회 (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 담당 강의(LECTURE.LMS_PRF_ID)의 주간 강의 시간표 — 학기가 from~to와 겹치는 비폐강 강의만 (날짜 전개는 service) */
    List<LmsProfCalendarDto.LectureTimeRow> selectLectureTimes(@Param("lmsPrfId") Long lmsPrfId,
                                                               @Param("from") String from,
                                                               @Param("to") String to);

    /** 담당 강의의 과제 마감 — from~to 범위 내, 비폐강 강의만 */
    List<LmsProfCalendarDto.AssignmentDueRow> selectAssignmentDues(@Param("lmsPrfId") Long lmsPrfId,
                                                                  @Param("from") String from,
                                                                  @Param("to") String to);
}
