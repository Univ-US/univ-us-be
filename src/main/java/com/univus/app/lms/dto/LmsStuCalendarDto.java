package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SLM-010 학생 캘린더 DTO 묶음 (3분류 네스티드 — CLAUDE-BE §9 #12). 학생 전용(수강 강의).
 * <p>매핑 row(LectureTimeRow=주간 시간표 / AssignmentDueRow=과제 마감) + 응답(EventResDto = FE CalendarEvent 계약).
 * service가 주간 시간표 row를 from~to 범위로 날짜 전개(학기 경계 clamp, DAY_CODE→요일)하고 과제 마감을 합쳐 EventResDto[]로 변환.
 * <p>표시 규칙(FE 확정): 강의=과목명+분반+시작~종료 / 과제=과제명+마감시각(분반·종료시각 없음/null).
 * <p>⚠️ 교수 캘린더(LmsProfCalendarDto)와 동일 JSON 계약(CalendarEvent)이되 '내 강의=수강(LECTURE_STUDENT_ENROLLMENT, status!='DRP')' 술어만 다름 — 소유경계 분리로 자족 복제.
 * MyBatis resultType은 {@code ...LmsStuCalendarDto$Inner} 형태로 참조.
 */
public final class LmsStuCalendarDto {

    private LmsStuCalendarDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType, 내부 전용 ===== */

    /** 주간 강의 시간표 1행 매핑 (selectLectureTimes) — service가 요일·학기경계로 날짜 전개 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureTimeRow {
        private Long lecId;
        private String courseName;   // LECTURE_CODE.LEC_COD_NAME
        private Integer lecSection;  // LECTURE.LEC_SECTION (분반)
        private String dayCode;      // LECTURE_TIME.LEC_TIM_DAY_CODE (MON~SUN)
        private String startTime;    // "HH:mm" (TO_CHAR LEC_TIM_STR_TIME)
        private String endTime;      // "HH:mm" (TO_CHAR LEC_TIM_END_TIME)
        private String semStartDate; // "YYYY-MM-DD" (SEMESTERS.SEM_STR_DATE) — 전개 하한 경계
        private String semEndDate;   // "YYYY-MM-DD" (SEMESTERS.SEM_END_DATE) — 전개 상한 경계
    }

    /** 과제 마감 1행 매핑 (selectAssignmentDues) — 범위 내 마감일/시각 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentDueRow {
        private Long lecId;
        private String title;   // LECTURE_ASSIGNMENT.LEC_ASN_TITLE (과제명)
        private String dueDate; // "YYYY-MM-DD" (TO_CHAR LEC_ASN_DUE_DATE)
        private String dueTime; // "HH:mm" (TO_CHAR LEC_ASN_DUE_DATE)
    }

    /* ===== 응답(ResDto) — JSON 프로퍼티명 = FE 계약(CalendarEvent, 불변) ===== */

    /** 캘린더 이벤트 1건 — FE {@code CalendarEvent}(date·type·title·time·endTime·lecId·lecSection) 계약 그대로 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventResDto {
        private String date;        // "YYYY-MM-DD"
        private String type;        // "LECTURE" | "ASSIGNMENT"
        private String title;       // 강의=과목명 / 과제=과제명
        private String time;        // "HH:mm" — 강의=시작 시각 / 과제=마감 시각
        private String endTime;     // "HH:mm" — 강의 종료 시각 / 과제=null
        private Long lecId;         // 과목 식별
        private Integer lecSection; // 분반 — 강의만 / 과제=null
    }
}
