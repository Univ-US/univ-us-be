package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfAttendanceDto;

import java.util.List;

/* PLM-008 교수 출결 관리 Service */
public interface LmsProfAttendanceService {

    /** 교수 담당 강의 드롭다운 (학기 무관 전체 + 수강생 수). LMS 프로필 없으면 빈 목록 */
    List<LmsProfAttendanceDto.LectureResDto> getLectures(Long professorMemberId);

    /** 강의 1개 출결 — 강의 헤더 + 요약(출석/지각/결석 평균, 합100) + 학생별 회차. 본인 강의 아니면 403 */
    LmsProfAttendanceDto.LectureAttendanceResDto getLectureAttendance(Long professorMemberId, Long lecId);

    /** 학생 회차별 출결 상태 수정 → 갱신된 학생 행 반환. 본인 강의·수강생 아니면 403/404, 잘못된 상태 400 */
    LmsProfAttendanceDto.StudentResDto updateStudentAttendance(Long professorMemberId, Long lecId,
                                                               Long studentMemberId,
                                                               List<LmsProfAttendanceDto.SessionUpdateDto> sessions);
}
