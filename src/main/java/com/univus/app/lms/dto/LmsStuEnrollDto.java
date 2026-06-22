package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 학생 "수강신청" DTO 묶음.
 * <ul>
 *   <li>{@code LectureFlatRow} = MyBatis 매핑 row (개설 강좌 1건 + 시간표 flat, 미직렬화)</li>
 *   <li>{@code LectureRow} = JSON 응답 (FE 계약 필드명)</li>
 * </ul>
 */
public final class LmsStuEnrollDto {

    private LmsStuEnrollDto() {
    }

    /* ===== GET /api/lms/student/enroll/summary ===== */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryResDto {
        private String semesterLabel;
        private int maxCredit;
    }

    /** 수강신청 대상 학기 (개설(OPEN) 강좌가 속한 학기) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TargetSemesterRow {
        private Long semId;
        private Integer semYear;
        private String semTerm;
    }

    /* ===== GET /api/lms/student/enroll/lectures ===== */

    /** 개설 강좌 flat 행 (MyBatis SELECT resultType, 미직렬화) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureFlatRow {
        private Long lecId;             // LECTURE.LEC_ID
        private String courseCode;      // LECTURE_CODE.LEC_CODE (학수번호)
        private String courseName;      // LECTURE_CODE.LEC_COD_NAME
        private String professor;       // MEMBER.MEMBER_NAME (조인)
        private String department;      // DEPARTMENT.DEPT_NAME (조인)
        private Integer lecCredit;       // LECTURE.LEC_CREDIT
        private Integer lecSection;      // LECTURE.LEC_SECTION
        private Integer capacity;        // LECTURE.LEC_CAPACITY
        private Integer enrolledCount;   // LECTURE_STUDENT_ENROLLMENT COUNT (DRP 제외)
        private Integer alreadyEnrolledFlag; // 1=현재 학생이 이미 신청(DRP 제외) / 0=아님
        private String lecTimDayCode;    // LECTURE_TIME.LEC_TIM_DAY_CODE
        private String lecTimStrTime;    // LECTURE_TIME.LEC_TIM_STR_TIME (HH:MI)
        private String lecTimEndTime;    // LECTURE_TIME.LEC_TIM_END_TIME (HH:MI)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleSlot {
        private String day;   // 월/화/수/목/금/토/일
        private String start; // "HH:mm"
        private String end;   // "HH:mm"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureRow {
        private Long lecId;
        private String courseCode;
        private String courseName;
        private String professor;
        private String department;
        private Integer lecCredit;
        private Integer lecSection;
        private String schedule;            // "월 09:00-10:30 / 수 09:00-10:30", 없으면 "-"
        private List<ScheduleSlot> scheduleSlots;
        private Integer capacity;
        private Integer enrolledCount;
        private boolean alreadyEnrolled;
    }

    /* ===== 신청 처리용 내부 조회 row ===== */

    /** 신청 대상 강좌 검증용 (POST /enroll 단건 조회) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureForEnrollRow {
        private Long lecId;
        private Long semId;
        private Long univId;
        private Integer lecCredit;
        private Integer capacity;
        private Integer enrolledCount;
        private String lecValStatus;
    }

    /** 강좌 시간표 슬롯 (요일/시간) — 충돌 검사용 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSlotRow {
        private String dayCode;  // MON/TUE/WED/THU/FRI/SAT/SUN
        private String startTime; // HH:MI
        private String endTime;   // HH:MI
    }

    /** 기존 신청 내역 (LMS_PRF_ID + LEC_ID 단건, 재신청/이미신청 판별용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentRow {
        private Long lecStdEnrId;
        private String status; // ENR/CMP/DRP
    }

    /* ===== POST /api/lms/student/enroll ===== */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitReqDto {
        private List<Long> lecIds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FailedItem {
        private Long lecId;
        private String reason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitResultDto {
        private List<Long> success;
        private List<FailedItem> failed;
    }
}
