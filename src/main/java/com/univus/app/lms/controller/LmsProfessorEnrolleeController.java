package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsLectureDto;
import com.univus.app.lms.dto.LmsLectureStudentsResponseDto;
import com.univus.app.lms.dto.LmsSemesterResponseDto;
import com.univus.app.lms.dto.LmsStudentReportDto;
import com.univus.app.lms.service.LmsProfessorEnrolleeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /** PLM-003 : 강의별 수강생 목록 + 통계 (이름/학번 검색 optional) */
    // GET /api/lms/professor/lectures/{lecId}/students?search=
    @GetMapping("/lectures/{lecId}/students")
    public ResponseEntity<LmsLectureStudentsResponseDto> requestGetLectureStudents(
            Authentication authentication,
            @PathVariable Long lecId,
            @RequestParam(value = "search", required = false) String search) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorEnrolleeService.getLectureStudents(memberId, lecId, search));
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
