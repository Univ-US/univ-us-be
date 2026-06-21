package com.univus.app.lms.controller;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuCoursesDto;
import com.univus.app.lms.service.LmsStuCoursesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lms/student")
@RequiredArgsConstructor
public class LmsStuCoursesController {

    private final LmsStuCoursesService lmsStuCoursesService;

    /** 전체 수강 내역 (대시보드 합성용 — 미페이징) */
    @GetMapping("/courses")
    public ResponseEntity<List<LmsStuCoursesDto.SemesterCoursesResDto>> requestGetStudentCourses(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuCoursesService.getCourses(memberId));
    }

    /** 전 학기 요약 (카드 헤더 — 과목은 학기별 페이지 조회) */
    // GET /api/lms/student/courses/semesters
    @GetMapping("/courses/semesters")
    public ResponseEntity<List<LmsStuCoursesDto.SemesterSummaryResDto>> requestGetStudentSemesters(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuCoursesService.getSemesterSummaries(memberId));
    }

    /** 한 학기(semId)의 과목 1페이지 — 서버 페이지네이션. page 0-based, size 기본 10 */
    // GET /api/lms/student/courses/semesters/{semId}?page=&size=
    @GetMapping("/courses/semesters/{semId}")
    public ResponseEntity<PaginateUtilRestApiRes<LmsStuCoursesDto.CourseRow>> requestGetSemesterCourses(
            Authentication authentication,
            @PathVariable Long semId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        return ResponseEntity.ok(lmsStuCoursesService.getSemesterCoursesPaged(memberId, semId, p, s));
    }
}
