package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsProfCalendarDto;
import com.univus.app.lms.service.LmsProfCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* PLM-010 교수 캘린더 Controller — 담당 강의 일정 + 과제 마감 (교수 전용) */
@RestController
@RequestMapping("/api/lms/professor/calendar")
@RequiredArgsConstructor
public class LmsProfCalendarController {

    private final LmsProfCalendarService lmsProfCalendarService;

    /** 담당 강의의 강의일정 + 과제마감 (from~to "YYYY-MM-DD", 포함) */
    // GET /api/lms/professor/calendar?from=&to=
    @GetMapping
    public ResponseEntity<List<LmsProfCalendarDto.EventResDto>> requestGetCalendar(
            Authentication authentication,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfCalendarService.getCalendar(memberId, from, to));
    }
}
