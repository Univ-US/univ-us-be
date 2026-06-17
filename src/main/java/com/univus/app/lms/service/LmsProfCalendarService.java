package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfCalendarDto;

import java.util.List;

/** PLM-010 교수 캘린더 Service (교수 전용) — 담당 강의 일정 + 과제 마감 통합 */
public interface LmsProfCalendarService {

    /** 담당 강의(LMS_PRF_ID)의 강의일정 + 과제마감 (from~to "YYYY-MM-DD", 포함) */
    List<LmsProfCalendarDto.EventResDto> getCalendar(Long memberId, String from, String to);
}
