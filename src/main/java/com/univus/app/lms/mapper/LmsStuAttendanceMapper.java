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

    /** 수강 강의별 출석 집계 (전체 — 대시보드 합성·미페이징). */
    List<LmsStuAttendanceDto.CourseRow> selectCourseRows(@Param("lmsPrfId") Long lmsPrfId);

    /** 지각/결석 날짜 상세 (전체 — 대시보드 합성·미페이징). */
    List<LmsStuAttendanceDto.RecordRow> selectDetailRecords(@Param("lmsPrfId") Long lmsPrfId);

    /** 전 학기 요약 (카드 헤더 — 학기별 과목수·진행중, 최신순). */
    List<LmsStuAttendanceDto.SemesterSummaryRow> selectAttendanceSemesterSummaries(@Param("lmsPrfId") Long lmsPrfId);

    /** 한 학기의 과목(수강) 개수 — 페이지네이션 total. */
    long countSemesterCourses(@Param("lmsPrfId") Long lmsPrfId, @Param("semId") Long semId);

    /** 한 학기의 과목별 출석 집계 1페이지 (OFFSET/FETCH). */
    List<LmsStuAttendanceDto.CourseRow> selectSemesterCourseRowsPaged(@Param("lmsPrfId") Long lmsPrfId,
                                                                      @Param("semId") Long semId,
                                                                      @Param("offset") int offset,
                                                                      @Param("size") int size);

    /** 주어진 수강(enrollmentId)들의 지각/결석 날짜 상세 (페이지 과목 records 조립용). */
    List<LmsStuAttendanceDto.RecordRow> selectDetailRecordsByEnrollments(
            @Param("enrollmentIds") List<Long> enrollmentIds);
}
