package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * SLM-003 학생 "수강 내역" DTO 묶음. (SLM-002=대시보드는 전용 BE 없음)
 * <ul>
 *   <li>{@code CourseFlatRow} = MyBatis 매핑 row (강의 1건 + 학기 + 시간표 flat, 미직렬화)</li>
 *   <li>{@code CourseRow / SemesterCoursesResDto} = JSON 응답 (FE 계약 필드명)</li>
 * </ul>
 * 변수명 = DB 컬럼 카멜(자기/앵커) · 조인 = 의미별칭(courseName·professor).
 */
public final class LmsStuCoursesDto {

    private LmsStuCoursesDto() {
    }

    /* ===== 매핑 row (MyBatis SELECT resultType, 미직렬화) ===== */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseFlatRow {
        private Long semId;             // SEMESTERS.SEM_ID
        private Integer semYear;        // SEMESTERS.SEM_YEAR
        private String semTerm;         // SEMESTERS.SEM_TERM (SM1/SMR/SM2/WNT)
        private Integer inProgressFlag; // 1=진행중 학기(SYSDATE ∈ [STR,END]) / 0=아님
        private Long lecId;             // LECTURE.LEC_ID
        private String courseName;      // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;     // LECTURE.LEC_SECTION (분반)
        private Integer lecCredit;      // LECTURE.LEC_CREDIT (학점)
        private String professor;       // MEMBER.MEMBER_NAME (조인)
        private String lecStdEnrStatus; // LECTURE_STUDENT_ENROLLMENT.LEC_STD_ENR_STATUS (수강 상태: ENR/CMP/DRP/FAL)
        private String lecValStatus;    // LECTURE.LEC_VAL_STATUS (강의 상태: OPEN/PROG/CLSD/CNCL)
        private String lecTimDayCode;   // LECTURE_TIME.LEC_TIM_DAY_CODE
        private String lecTimStrTime;   // LECTURE_TIME.LEC_TIM_STR_TIME (HH:MI)
        private String lecTimEndTime;   // LECTURE_TIME.LEC_TIM_END_TIME (HH:MI)
    }

    /* ===== 응답 (GET /api/lms/student/courses) ===== */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseRow {
        private Long lecId;
        private String courseName;      // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;     // LECTURE.LEC_SECTION (분반)
        private Integer lecCredit;      // LECTURE.LEC_CREDIT (학점)
        private String professor;       // MEMBER.MEMBER_NAME (조인)
        private String lecStdEnrStatus; // 수강 상태 (ENR/CMP/DRP/FAL) — FE 공통코드 ENR_STS 라벨
        private String lecValStatus;    // 강의 상태 (OPEN/PROG/CLSD/CNCL) — FE 공통코드 LEC_VAL_STATUS 라벨
        private String schedule;        // 요일+시간 조합("월 10:00~12:00 · ..."), 없으면 "-"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterCoursesResDto {
        private Integer semYear;
        private String semTerm;
        private String semesterLabel;
        private boolean inProgress;
        private int courseCount;
        private int totalCredits;
        private List<CourseRow> courses;
    }

    /* ===== 학기 요약 (GET /courses/semesters — 카드 헤더, 과목은 학기별 페이지 조회) ===== */

    /** 학기 요약 매핑 row (GROUP BY 집계) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterSummaryRow {
        private Long semId;             // SEMESTERS.SEM_ID (학기별 과목 페이지 키)
        private Integer semYear;
        private String semTerm;
        private Integer inProgressFlag; // 1=진행중
        private int courseCount;        // 그 학기 과목 수
        private int totalCredits;       // 그 학기 총 학점
    }

    /** 학기 요약 응답 — courses 없이 헤더만(과목은 GET /courses/semesters/{semId} 페이지 조회) */
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
        private int totalCredits;
    }
}
