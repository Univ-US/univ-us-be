package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuCalendarDto;

import java.util.List;

/** SLM-010 학생 캘린더 Service (학생 전용) — 수강 강의 일정 + 과제 마감 통합 */
public interface LmsStuCalendarService {

    /** 수강 강의(LMS_PRF_ID)의 강의일정 + 과제마감 (from~to "YYYY-MM-DD", 포함) */
    List<LmsStuCalendarDto.EventResDto> getCalendar(Long memberId, String from, String to);
}
