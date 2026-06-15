package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuAttendanceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SLM-005 학생 출석 내역 Mapper. */
@Mapper
public interface LmsStuAttendanceMapper {

    /** 학생 본인 LMS_PRF_ID 조회. */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 수강 강의별 출석 집계. */
    List<LmsStuAttendanceDto.CourseRow> selectCourseRows(@Param("lmsPrfId") Long lmsPrfId);

    /** 지각/결석 날짜 상세. */
    List<LmsStuAttendanceDto.RecordRow> selectDetailRecords(@Param("lmsPrfId") Long lmsPrfId);
}
