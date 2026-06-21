package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuAttendanceDto;

import java.util.List;

/** SLM-005 학생 출석 내역 Service. */
public interface LmsStuAttendanceService {

    /** 로그인 학생의 수강 강의별 출석 내역을 학기별로 조회한다 (대시보드 합성용 — 미페이징). */
    List<LmsStuAttendanceDto.SemesterAttendanceResDto> getAttendance(Long memberId);

    /** 전 학기 요약 (카드 헤더 — 과목은 학기별 페이지 조회). */
    List<LmsStuAttendanceDto.SemesterSummaryResDto> getAttendanceSemesterSummaries(Long memberId);

    /** 한 학기(semId)의 과목별 출결 1페이지 (서버 페이지네이션). */
    PaginateUtilRestApiRes<LmsStuAttendanceDto.AttendanceCourseResDto> getSemesterAttendancePaged(
            Long memberId, Long semId, int page, int size);
}
