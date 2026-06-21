package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuCoursesDto;

import java.util.List;

public interface LmsStuCoursesService {

    /** 전체 학기 수강 내역 (대시보드 합성용 — 미페이징) */
    List<LmsStuCoursesDto.SemesterCoursesResDto> getCourses(Long memberId);

    /** 전 학기 요약 (카드 헤더 — 과목은 학기별 페이지 조회) */
    List<LmsStuCoursesDto.SemesterSummaryResDto> getSemesterSummaries(Long memberId);

    /** 한 학기(semId)의 과목 1페이지 (서버 페이지네이션) */
    PaginateUtilRestApiRes<LmsStuCoursesDto.CourseRow> getSemesterCoursesPaged(
            Long memberId, Long semId, int page, int size);
}
