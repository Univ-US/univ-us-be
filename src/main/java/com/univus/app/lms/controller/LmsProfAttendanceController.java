package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsProfAttendanceDto;
import com.univus.app.lms.service.LmsProfAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* PLM-008 교수 페이지 - 출결 관리 Controller (/api/lms/professor/** = PROF 가드, SecurityConfig 무변경) */
@RestController
@RequestMapping("/api/lms/professor/attendance")
@RequiredArgsConstructor
public class LmsProfAttendanceController {

    private final LmsProfAttendanceService lmsProfAttendanceService;

    /** 학기/강의 드롭다운 — 교수 담당 강의 전체 + 수강생 수 */
    // GET /api/lms/professor/attendance/lectures
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsProfAttendanceDto.LectureResDto>> requestGetLectures(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfAttendanceService.getLectures(memberId));
    }

    /** 강의 1개 출결 — 헤더 + 요약(출석/지각/결석 평균) + 학생별 회차 */
    // GET /api/lms/professor/attendance/lectures/{lecId}
    @GetMapping("/lectures/{lecId}")
    public ResponseEntity<LmsProfAttendanceDto.LectureAttendanceResDto> requestGetLectureAttendance(
            Authentication authentication,
            @PathVariable Long lecId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfAttendanceService.getLectureAttendance(memberId, lecId));
    }

    /** 학생 회차별 출결 상태 수정 → 갱신된 학생 행 */
    // PUT /api/lms/professor/attendance/lectures/{lecId}/students/{studentMemberId}
    //   body: { "sessions": [ { "sessionId": 1, "status": "LAT" }, ... ] }
    @PutMapping("/lectures/{lecId}/students/{studentMemberId}")
    public ResponseEntity<LmsProfAttendanceDto.StudentResDto> requestUpdateStudentAttendance(
            Authentication authentication,
            @PathVariable Long lecId,
            @PathVariable Long studentMemberId,
            @RequestBody LmsProfAttendanceDto.UpdateReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfAttendanceService.updateStudentAttendance(
                memberId, lecId, studentMemberId, request.getSessions()));
    }
}
