package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsLectureDto;
import com.univus.app.lms.dto.LmsLectureStudentsResponseDto;
import com.univus.app.lms.dto.LmsSemesterResponseDto;
import com.univus.app.lms.dto.LmsStudentReportDto;
import com.univus.app.lms.service.LmsProfessorEnrolleeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/* PLM-003 교수 페이지 - 수강생 현황 Controller */
//TODO: SpringSecurity ROLE 가드(PROF/ADM) 추가
@RestController
@RequestMapping("/api/lms/professor")
@RequiredArgsConstructor
public class LmsProfessorEnrolleeController {

    private final LmsProfessorEnrolleeService lmsProfessorEnrolleeService;

    /** 학기 드롭다운 */
    // GET /api/lms/professor/semesters
    @GetMapping("/semesters")
    public ResponseEntity<List<LmsSemesterResponseDto>> requestGetSemesters() {
        return ResponseEntity.ok(lmsProfessorEnrolleeService.getSemesters());
    }

    /** 교수 담당 강의 드롭다운 (semesterId optional) */
    // GET /api/lms/professor/lectures?semesterId=
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsLectureDto>> requestGetLectures(
            Authentication authentication,
            @RequestParam(value = "semesterId", required = false) Long semesterId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorEnrolleeService.getLectures(memberId, semesterId));
    }

    /** PLM-003 : 강의별 수강생 목록(페이지) + 통계 (검색·제출필터·정렬·페이지네이션 optional) */
    // GET /api/lms/professor/lectures/{lecId}/students?search=&submission=&sort=&order=&page=0&size=10
    //   search: 이름/학번 부분일치 · submission: complete|incomplete
    //   sort: attendance|score · order: asc|desc(기본 asc)
    //   page: 0-based(기본 0) · size: 페이지 크기(기본 10) — 통계 카드는 검색 전체 기준(정렬·제출필터·페이지 무관)
    @GetMapping("/lectures/{lecId}/students")
    public ResponseEntity<LmsLectureStudentsResponseDto> requestGetLectureStudents(
            Authentication authentication,
            @PathVariable Long lecId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "submission", required = false) String submission,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "order", required = false) String order,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorEnrolleeService.getLectureStudents(
                memberId, lecId, search, submission, sort, order, page, size));
    }

    /** PLM-003 : 수강생 명단 Excel(.xlsx) 내보내기 — 현재 검색·필터·정렬 결과 그대로 */
    // GET /api/lms/professor/lectures/{lecId}/students/export?search=&submission=&sort=&order=
    //   인증 필요(/api/lms/**). FE는 Authorization 헤더로 fetch → blob 다운로드(<a download>는 토큰 못 실음)
    @GetMapping("/lectures/{lecId}/students/export")
    public ResponseEntity<byte[]> requestExportLectureStudents(
            Authentication authentication,
            @PathVariable Long lecId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "submission", required = false) String submission,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "order", required = false) String order) {
        Long memberId = (Long) authentication.getPrincipal();
        byte[] body = lmsProfessorEnrolleeService.exportLectureStudentsExcel(
                memberId, lecId, search, submission, sort, order);

        String filename = "수강생명단_lec" + lecId + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"enrollees_lec" + lecId + ".xlsx\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /** PLM-003-01 : 수강생 상세 리포트 */
    // GET /api/lms/professor/lectures/{lecId}/students/{studentMemberId}/report
    @GetMapping("/lectures/{lecId}/students/{studentMemberId}/report")
    public ResponseEntity<LmsStudentReportDto> requestGetStudentReport(
            Authentication authentication,
            @PathVariable Long lecId,
            @PathVariable Long studentMemberId) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorEnrolleeService.getStudentReport(memberId, lecId, studentMemberId));
    }
}
