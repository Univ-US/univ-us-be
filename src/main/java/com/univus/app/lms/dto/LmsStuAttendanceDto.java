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
        private Integer year;
        private String termCode;
        private Integer inProgressFlag;
        private Long enrollmentId;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer totalSessions;
        private Integer present;
        private Integer late;
        private Integer absent;
        private Integer attendanceTotal;
    }

    /** 지각/결석 날짜 상세 row. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordRow {
        private Long enrollmentId;
        private String statusCode;
        private String date;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceRecordResDto {
        private String date;
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
        private Integer totalSessions;
        private Integer present;
        private Integer late;
        private Integer absent;
        private Integer attendanceRate;
        private List<AttendanceRecordResDto> lateRecords;
        private List<AttendanceRecordResDto> absentRecords;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterAttendanceResDto {
        private Integer year;
        private String termCode;
        private String semesterLabel;
        private Boolean inProgress;
        private Integer courseCount;
        private List<AttendanceCourseResDto> courses;
    }
}
