package com.univus.app.lms.controller;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuAttendanceDto;
import com.univus.app.lms.service.LmsStuAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** SLM-005 학생 출석 내역 Controller. */
@RestController
@RequestMapping("/api/lms/student/attendance")
@RequiredArgsConstructor
public class LmsStuAttendanceController {

    private final LmsStuAttendanceService lmsStuAttendanceService;

    /** 로그인 학생의 수강 강의별 출석 내역 (대시보드 합성용 — 미페이징). */
    @GetMapping
    public ResponseEntity<List<LmsStuAttendanceDto.SemesterAttendanceResDto>> requestGetAttendance(
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStuAttendanceService.getAttendance(memberId));
    }

    /** 전 학기 요약 (카드 헤더 — 과목은 학기별 페이지 조회). */
    // GET /api/lms/student/attendance/semesters
    @GetMapping("/semesters")
    public ResponseEntity<List<LmsStuAttendanceDto.SemesterSummaryResDto>> requestGetAttendanceSemesters(
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStuAttendanceService.getAttendanceSemesterSummaries(memberId));
    }

    /** 한 학기(semId)의 과목별 출결 1페이지 — 서버 페이지네이션. page 0-based, size 기본 10. */
    // GET /api/lms/student/attendance/semesters/{semId}?page=&size=
    @GetMapping("/semesters/{semId}")
    public ResponseEntity<PaginateUtilRestApiRes<LmsStuAttendanceDto.AttendanceCourseResDto>> requestGetSemesterAttendance(
            Authentication authentication,
            @PathVariable Long semId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        Long memberId = (Long) authentication.getPrincipal();
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        return ResponseEntity.ok(lmsStuAttendanceService.getSemesterAttendancePaged(memberId, semId, p, s));
    }
}
