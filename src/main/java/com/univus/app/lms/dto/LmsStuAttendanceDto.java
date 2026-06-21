package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** SLM-005 학생 출석 내역 DTO 묶음. */
public final class LmsStuAttendanceDto {

    private LmsStuAttendanceDto() {
    }

    /** 수강 강의별 출석 집계 row. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseRow {
        private Long semId;               // SEMESTERS.SEM_ID (학기별 페이지 조회 키)
        private Integer semYear;          // SEMESTERS.SEM_YEAR
        private String semTerm;           // SEMESTERS.SEM_TERM
        private Integer inProgressFlag;   // 파생(SYSDATE ∈ 학기범위)
        private Long enrollmentId;        // LECTURE_STUDENT_ENROLLMENT.LEC_STD_ENR_ID (PK)
        private Long lecId;               // LECTURE.LEC_ID
        private String courseName;        // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;       // LECTURE.LEC_SECTION (분반)
        private Integer lecTotClasses;    // LECTURE.LEC_TOT_CLASSES (총 강의 회차)
        private Integer present;          // 집계 COUNT(PRS)
        private Integer late;             // 집계 COUNT(LAT)
        private Integer absent;           // 집계 COUNT(ABS)
        private Integer attendanceTotal;  // 집계 COUNT(전체) — 출석률 분모
    }

    /** 지각/결석 날짜 상세 row. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordRow {
        private Long enrollmentId;        // LEC_STD_ENR_ID (그룹핑 키)
        private String stdEnrAtdStsCode;  // STUDENT_ENROLLMENT_ATTENDANCE.STD_ENR_ATD_STS_CODE
        private String stdEnrAtdRegDate;  // STD_ENR_ATD_REG_DATE (TO_CHAR 'YYYY-MM-DD')
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceRecordResDto {
        private String stdEnrAtdRegDate;  // STD_ENR_ATD_REG_DATE (지각/결석 날짜)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceCourseResDto {
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer lecTotClasses;    // LECTURE.LEC_TOT_CLASSES (총 강의 회차)
        private Integer present;
        private Integer late;
        private Integer absent;
        private Integer attendanceRate;   // 파생 %
        private List<AttendanceRecordResDto> lateRecords;
        private List<AttendanceRecordResDto> absentRecords;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterAttendanceResDto {
        private Integer semYear;
        private String semTerm;
        private String semesterLabel;
        private Boolean inProgress;
        private Integer courseCount;
        private List<AttendanceCourseResDto> courses;
    }

    /* ===== 학기별 출결 페이지네이션 (카드 헤더는 요약, 과목은 학기별 페이지 조회) ===== */

    /** 학기 요약 매핑 row (GROUP BY 집계) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterSummaryRow {
        private Long semId;
        private Integer semYear;
        private String semTerm;
        private Integer inProgressFlag;
        private int courseCount;
    }

    /** 학기 요약 응답 (카드 헤더 — 과목은 GET /attendance/semesters/{semId} 페이지 조회) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterSummaryResDto {
        private Long semId;
        private Integer semYear;
        private String semTerm;
        private String semesterLabel;
        private boolean inProgress;
        private int courseCount;
    }
}
