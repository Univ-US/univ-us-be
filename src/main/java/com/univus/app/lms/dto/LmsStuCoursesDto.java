package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public final class LmsStuCoursesDto {

    private LmsStuCoursesDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseFlatRow {
        private Long semId;
        private Integer year;
        private String termCode;
        private Integer inProgressFlag;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer credit;
        private String professor;
        private String dayCode;
        private String startTime;
        private String endTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseRow {
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer credit;
        private String professor;
        private String schedule;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterCoursesResDto {
        private Integer year;
        private String termCode;
        private String semesterLabel;
        private boolean inProgress;
        private int courseCount;
        private int totalCredits;
        private List<CourseRow> courses;
    }
}
