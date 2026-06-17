package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuCalendarDto;
import com.univus.app.lms.service.LmsStuCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* SLM-010 학생 캘린더 Controller — 수강 강의 일정 + 과제 마감 (학생 전용) */
@RestController
@RequestMapping("/api/lms/student/calendar")
@RequiredArgsConstructor
public class LmsStuCalendarController {

    private final LmsStuCalendarService lmsStuCalendarService;

    /** 수강 강의의 강의일정 + 과제마감 (from~to "YYYY-MM-DD", 포함) */
    // GET /api/lms/student/calendar?from=&to=
    @GetMapping
    public ResponseEntity<List<LmsStuCalendarDto.EventResDto>> requestGetCalendar(
            Authentication authentication,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStuCalendarService.getCalendar(memberId, from, to));
    }
}
