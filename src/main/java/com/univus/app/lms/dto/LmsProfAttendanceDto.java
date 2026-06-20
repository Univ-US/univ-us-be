package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PLM-008 교수 "출결 관리" DTO 묶음 (3분류 네스티드).
 * <p>강의별 학생 출석·지각·결석 조회 및 회차별 상태 수정.
 * <p>매핑 row(StudentRow/SessionRow) = MyBatis resultType, 내부 전용.
 * 응답(LectureResDto/StudentResDto/SessionResDto/SummaryResDto/LectureAttendanceResDto) = JSON 프로퍼티명 = FE 계약(불변).
 * 요청(UpdateReqDto/SessionUpdateDto) = PUT body.
 * <p>출결 상태(ATTD_STS)는 DB상 5종(PRS/LAT/ABS/ELV/EXC)이나 ELV·EXC는 VAL_STATUS=DEL(미사용).
 * 이 화면은 출석/지각/결석 3종(PRS/LAT/ABS)만 다룬다.
 * <p>회차(수업일) = STUDENT_ENROLLMENT_ATTENDANCE 1행, 날짜 = STD_ENR_ATD_REG_DATE(전용 수업일 컬럼 없음).
 * <p>명명 규칙: DTO 변수명 = DB 컬럼명 카멜(자기/앵커 테이블), 조인/파생/집계는 의미 별칭 — 커뮤니티 PostDto 컨벤션.
 *    자기 컬럼(STUDENT_ENROLLMENT_ATTENDANCE) status→stdEnrAtdStsCode·날짜→stdEnrAtdRegDate, 학기 조인 year→semYear·termCode→semTerm.
 *    PK/ID(sessionId·enrollmentId·memberId)·조인 의미명(studentName·studentNo·lecName)·집계(present·attendanceRate 등)는 의미명 유지,
 *    매퍼 @Param·XML #{status}(updateSessionStatus)는 내부 쓰기 플러밍이라 유지.
 */
public final class LmsProfAttendanceDto {

    private LmsProfAttendanceDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType, 내부 전용 ===== */

    /** 수강생 1명 (selectStudents) — 카운트/회차는 SessionRow에서 Java 집계 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentRow {
        private Long enrollmentId; // LECTURE_STUDENT_ENROLLMENT.LEC_STD_ENR_ID
        private Long memberId;     // 학생 MEMBER_ID
        private String studentName;
        private String studentNo;  // MEMBER.LOGIN_ID (학번)
        private String imageUrl;   // 최신 유효 프로필 이미지 URL (nullable)
    }

    /** 출결 1회차 (selectSessions) — 평면 조회 후 service가 enrollmentId로 그룹핑 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionRow {
        private Long enrollmentId;  // 그룹핑 키 (LEC_STD_ENR_ID)
        private Long sessionId;          // STUDENT_ENROLLMENT_ATTENDANCE.STD_ENR_ATD_ID (자기 PK — 의미명 유지)
        private String stdEnrAtdRegDate; // STD_ENR_ATD_REG_DATE TO_CHAR 'YYYY-MM-DD' (자기 컬럼, alias date는 Oracle 예약어 ORA-00923 회피)
        private String stdEnrAtdStsCode; // STD_ENR_ATD_STS_CODE (자기 컬럼, PRS/LAT/ABS)
    }

    /* ===== 응답(ResDto) — JSON 프로퍼티명 = FE 계약(불변) ===== */

    /** 학기/강의 드롭다운 항목 + 선택 강의 헤더 (= FE AttendanceLecture) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureResDto {
        private Long lecId;
        private String lecName;       // LECTURE_CODE.LEC_COD_NAME
        private Integer lecSection;   // LECTURE.LEC_SECTION (분반)
        private Integer semYear;      // SEMESTERS.SEM_YEAR
        private String semTerm;       // SEMESTERS.SEM_TERM (SM1/SMR/SM2/WNT)
        private Integer studentCount; // 수강생 수 (status != 'DRP') — 집계
    }

    /** 회차 1건 (= FE AttendanceSession) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionResDto {
        private Long sessionId;
        private String stdEnrAtdRegDate; // "YYYY-MM-DD" (STD_ENR_ATD_REG_DATE)
        private String stdEnrAtdStsCode; // PRS/LAT/ABS (STD_ENR_ATD_STS_CODE)
    }

    /** 학생 1명 출결 (= FE AttendanceStudentRow) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentResDto {
        private Long enrollmentId;
        private Long memberId;
        private String studentName;
        private String studentNo;
        private String imageUrl;
        private int totalSessions;     // 총 회차 (PRS+LAT+ABS)
        private int present;           // 출석
        private int late;              // 지각
        private int absent;            // 결석
        private int attendanceRate;    // 출석률(%) — service 계산 round(present*100/total)
        private List<SessionResDto> sessions; // 회차별 상세
    }

    /** 통계 카드 — 평균 출석률 + 평균 지각률 + 평균 결석률 = 100% (= FE AttendanceSummary) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryResDto {
        private int totalStudents;
        private int averageAttendanceRate; // 학생별 출석률 평균(반올림)
        private int averageLateRate;       // 학생별 지각률 평균(반올림)
        private int averageAbsentRate;     // 100 − 출석 − 지각 (잔여 보정 → 셋 합 100%)
    }

    /** 강의 1개 출결 응답 (= FE LectureAttendance) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureAttendanceResDto {
        private LectureResDto lecture;
        private SummaryResDto summary;
        private List<StudentResDto> students;
    }

    /* ===== 요청(ReqDto) — PUT body ===== */

    /** 회차별 출결 수정 요청 (FE가 수정 모달의 전체 회차를 보냄, date는 무시) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReqDto {
        private List<SessionUpdateDto> sessions;
    }

    /** 회차 1건 변경 (sessionId = STD_ENR_ATD_ID, status = PRS/LAT/ABS) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionUpdateDto {
        private Long sessionId;
        private String stdEnrAtdStsCode; // PRS/LAT/ABS (요청 body, STD_ENR_ATD_STS_CODE)
    }
}
