package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuCoursesDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuCoursesMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 전체 수강 강의 flat 행 (대시보드 합성·미페이징) */
    List<LmsStuCoursesDto.CourseFlatRow> selectStudentCourseRows(@Param("lmsPrfId") Long lmsPrfId);

    /** 전 학기 요약 (카드 헤더 — 학기별 과목수·총학점 집계, 최신순) */
    List<LmsStuCoursesDto.SemesterSummaryRow> selectStudentSemesterSummaries(@Param("lmsPrfId") Long lmsPrfId);

    /** 한 학기의 과목(LEC_ID) 개수 — 학기별 과목 페이지네이션 total (소유권 lmsPrfId 스코프) */
    long countSemesterCourses(@Param("lmsPrfId") Long lmsPrfId, @Param("semId") Long semId);

    /** 한 학기의 과목 LEC_ID 1페이지 (OFFSET/FETCH) */
    List<Long> selectSemesterCourseLecIdsPaged(@Param("lmsPrfId") Long lmsPrfId,
                                               @Param("semId") Long semId,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    /** 주어진 LEC_ID들의 수강 강의 flat 행 (과목 페이지 재조립용 — 소유권 lmsPrfId 스코프) */
    List<LmsStuCoursesDto.CourseFlatRow> selectStudentCourseRowsByLecIds(@Param("lmsPrfId") Long lmsPrfId,
                                                                         @Param("lecIds") List<Long> lecIds);
}
