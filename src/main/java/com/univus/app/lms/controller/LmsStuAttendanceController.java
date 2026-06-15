package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuAttendanceDto;
import com.univus.app.lms.service.LmsStuAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** SLM-005 학생 출석 내역 Controller. */
@RestController
@RequestMapping("/api/lms/student/attendance")
@RequiredArgsConstructor
public class LmsStuAttendanceController {

    private final LmsStuAttendanceService lmsStuAttendanceService;

    /** 로그인 학생의 수강 강의별 출석 내역. */
    @GetMapping
    public ResponseEntity<List<LmsStuAttendanceDto.SemesterAttendanceResDto>> requestGetAttendance(
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStuAttendanceService.getAttendance(memberId));
    }
}
