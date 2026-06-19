package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * PLM-002 교수 "강의 내역" DTO 묶음 (3분류 네스티드).
 * <ul>
 *   <li>{@code CourseFlatRow} = MyBatis 매핑 row (강의 1건 + 학기 정보 + 집계값 flat)</li>
 *   <li>{@code ResDto / OverviewResDto / SemesterResDto / CourseResDto} = JSON 응답 (FE 계약 필드명)</li>
 * </ul>
 * 응답 구조: {@code { overview, semesters:[{ ..., courses:[...] }] }}
 * <p>KPI(overview)는 "진행중 학기 1개" 기준(년도·학기 필터와 무관 고정). 집계 계약(2026-06-17 확정):
 * 총 수강생 = 연인원(강의별 수강생 COUNT 단순 합, 중복 허용) · 평균 출석률 = 강의별 단순평균.
 * <p>※ courseType(이수구분)은 DB에 컬럼이 없어 미포함(2026-06-17 화면에서 제거 결정).
 */
public final class LmsProfCoursesDto {

    private LmsProfCoursesDto() {
    }

    /* ===== 매핑 row (MyBatis SELECT resultType) ===== */

    /** 강의 1건 (학기 정보 + 집계값 flat). selectCoursesFlat */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseFlatRow {
        private Long lecId;             // LECTURE.LEC_ID
        private Long semId;             // SEMESTERS.SEM_ID
        private Integer semYear;           // SEMESTERS.SEM_YEAR
        private String semTerm;        // SEMESTERS.SEM_TERM (SM1/SM2/SMR/WNT)
        private int inProgressFlag;     // 1=진행중 학기(SYSDATE ∈ [STR,END]) / 0=아님
        private String courseName;      // LECTURE_CODE.LEC_COD_NAME
        private Integer lecSection;     // LECTURE.LEC_SECTION (분반)
        private String schedule;        // LECTURE_TIME LISTAGG ("월 10:00 · 수 10:00"), 시간표 없으면 null
        private int studentCount;       // 수강생 수 (LEC_STD_ENR_STATUS != 'DRP')
        private Integer attendanceRate; // 강의 평균 출석률(학생별 출석률의 평균), 출석기록 없으면 null
        private int ungradedCount;      // 미채점 = Σ GREATEST(총원 - 채점완료, 0)
    }

    /* ===== 응답 (GET /api/lms/professor/courses) ===== */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResDto {
        private OverviewResDto overview;
        private List<SemesterResDto> semesters;
    }

    /** 상단 KPI 요약 (진행중 학기 기준) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewResDto {
        private int courseCount;        // 담당 강의 수
        private int studentTotal;       // 총 수강생 (연인원 합)
        private int ungradedTotal;      // 미채점 과제 합
        private int avgAttendanceRate;  // 평균 출석률 (강의 단순평균, %)
    }

    /** 학기 카드 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterResDto {
        private int semYear;
        private String semTerm;
        private String semesterLabel;   // "2026년 1학기"
        private boolean inProgress;
        private int courseCount;        // 학기 강의 수 (화면 미사용이나 FE 타입 계약 유지)
        private int studentTotal;       // 학기 수강생 합 (동상)
        private List<CourseResDto> courses;
    }

    /** 학기 카드 안 강의 1행 (courseType 미포함 — DB 미보유) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseResDto {
        private Long lecId;
        private String courseName;
        private int lecSection;            // 분반 (LECTURE.LEC_SECTION)
        private int studentCount;
        private String schedule;        // 시간표 없으면 "" (FE는 schedule || "-")
        private int attendanceRate;     // %
        private int ungradedCount;
    }
}
