package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuAttendanceDto;

import java.util.List;

/** SLM-005 학생 출석 내역 Service. */
public interface LmsStuAttendanceService {

    /** 로그인 학생의 수강 강의별 출석 내역을 학기별로 조회한다. */
    List<LmsStuAttendanceDto.SemesterAttendanceResDto> getAttendance(Long memberId);
}
