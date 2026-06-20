package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PLM-010 교수 캘린더 DTO 묶음 (3분류 네스티드). 교수 전용(담당 강의).
 * <p>매핑 row(LectureTimeRow=주간 시간표 / AssignmentDueRow=과제 마감) + 응답(EventResDto = FE CalendarEvent 계약).
 * service가 주간 시간표 row를 from~to 범위로 날짜 전개(학기 경계 clamp, DAY_CODE→요일)하고 과제 마감을 합쳐 EventResDto[]로 변환.
 * <p>표시 규칙(FE 확정): 강의=과목명+분반+시작~종료 / 과제=과제명+마감시각(분반·종료시각 없음/null).
 * MyBatis resultType은 {@code ...LmsProfCalendarDto$Inner} 형태로 참조.
 */
public final class LmsProfCalendarDto {

    private LmsProfCalendarDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType, 내부 전용 ===== */

    /** 주간 강의 시간표 1행 매핑 (selectLectureTimes) — service가 요일·학기경계로 날짜 전개 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureTimeRow {
        private Long lecId;            // LECTURE.LEC_ID
        private String courseName;     // LECTURE_CODE.LEC_COD_NAME (조인 의미별칭)
        private Integer lecSection;    // LECTURE.LEC_SECTION (분반)
        private String lecTimDayCode;  // LECTURE_TIME.LEC_TIM_DAY_CODE (MON~SUN)
        private String lecTimStrTime;  // "HH:mm" (TO_CHAR LEC_TIM_STR_TIME)
        private String lecTimEndTime;  // "HH:mm" (TO_CHAR LEC_TIM_END_TIME)
        private String semStrDate;     // "YYYY-MM-DD" (SEMESTERS.SEM_STR_DATE) — 전개 하한 경계
        private String semEndDate;     // "YYYY-MM-DD" (SEMESTERS.SEM_END_DATE) — 전개 상한 경계
    }

    /** 과제 마감 1행 매핑 (selectAssignmentDues) — 범위 내 마감일/시각 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentDueRow {
        private Long lecId;          // LECTURE.LEC_ID
        private String lecAsnTitle;    // LECTURE_ASSIGNMENT.LEC_ASN_TITLE (과제명)
        private String lecAsnDueDate;  // "YYYY-MM-DD" (TO_CHAR LEC_ASN_DUE_DATE — 날짜부)
        private String lecAsnDueTime;  // "HH:mm" (TO_CHAR LEC_ASN_DUE_DATE — 시각부)
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
