package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsProfCoursesDto;
import com.univus.app.lms.service.LmsProfCoursesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* PLM-002 교수 "강의 내역" — 학기별 담당 강의 + 진행중 학기 KPI (GET /api/lms/professor/courses) */
@RestController
@RequestMapping("/api/lms/professor")
@RequiredArgsConstructor
public class LmsProfCoursesController {

    private final LmsProfCoursesService lmsProfCoursesService;

    /**
     * 교수 강의 내역 조회.
     * GET /api/lms/professor/courses
     * 응답: { overview:{courseCount,studentTotal,ungradedTotal,avgAttendanceRate},
     *        semesters:[{year,termCode,semesterLabel,inProgress,courseCount,studentTotal,courses:[...]}] }
     */
    @GetMapping("/courses")
    public ResponseEntity<LmsProfCoursesDto.ResDto> requestGetCourses(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfCoursesService.getCourses(memberId));
    }
}
