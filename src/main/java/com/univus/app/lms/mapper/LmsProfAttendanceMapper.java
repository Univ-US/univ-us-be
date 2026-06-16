package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfAttendanceDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/* PLM-008 교수 출결 관리 Mapper */
@Mapper
public interface LmsProfAttendanceMapper {

    /* 교수 본인 LMS_PRF_ID (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /* 교수 담당 강의 전체(드롭다운) + 수강생 수 (학기 무관 — FE가 년도/학기로 좁힘) */
    List<LmsProfAttendanceDto.LectureResDto> selectLectures(@Param("lmsPrfId") Long lmsPrfId);

    /* 강의 헤더(+소유권 검증) + 수강생 수. 본인 강의 아니면 null */
    LmsProfAttendanceDto.LectureResDto selectLectureDetail(@Param("lecId") Long lecId,
                                                           @Param("professorLmsPrfId") Long professorLmsPrfId);

    /* 강의 수강생 목록 (status != 'DRP') */
    List<LmsProfAttendanceDto.StudentRow> selectStudents(@Param("lecId") Long lecId,
                                                         @Param("professorLmsPrfId") Long professorLmsPrfId);

    /* 강의 전체 출결 회차(평면) — PRS/LAT/ABS만, 날짜순. service가 enrollmentId로 그룹핑 */
    List<LmsProfAttendanceDto.SessionRow> selectSessions(@Param("lecId") Long lecId,
                                                         @Param("professorLmsPrfId") Long professorLmsPrfId);

    /* PUT 대상 학생의 수강신청 ID (본인 강의·해당 학생·미DRP). 없으면 null */
    Long findEnrollmentId(@Param("lecId") Long lecId,
                          @Param("professorLmsPrfId") Long professorLmsPrfId,
                          @Param("studentMemberId") Long studentMemberId);

    /* 회차 1건 상태 변경 — enrollmentId 스코프로 타 학생 회차 변경 차단(IDOR 방지). 영향 행 수 반환 */
    int updateSessionStatus(@Param("sessionId") Long sessionId,
                            @Param("enrollmentId") Long enrollmentId,
                            @Param("status") String status);

    /* PUT 응답용 — 학생 1명 기본 정보 */
    LmsProfAttendanceDto.StudentRow selectStudentByMember(@Param("lecId") Long lecId,
                                                          @Param("professorLmsPrfId") Long professorLmsPrfId,
                                                          @Param("studentMemberId") Long studentMemberId);

    /* PUT 응답용 — 한 수강신청의 회차 전체(PRS/LAT/ABS, 날짜순) */
    List<LmsProfAttendanceDto.SessionRow> selectSessionsByEnrollment(@Param("enrollmentId") Long enrollmentId);
}
