package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfCoursesDto;

/** PLM-002 교수 "강의 내역" 서비스 */
public interface LmsProfCoursesService {

    /** 교수 강의 내역 (학기별 강의 목록 + 진행중 학기 KPI) */
    LmsProfCoursesDto.ResDto getCourses(Long memberId);
}
