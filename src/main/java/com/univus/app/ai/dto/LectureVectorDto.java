package com.univus.app.ai.dto;

import lombok.Data;

@Data
public class LectureVectorDto {
    private Long lecId;
    private Long univId;
    private String lecCodName;
    private String lecCode;
    private String deptName;
    private String professorName;
    private Integer lecCredit;
    private String dayCodes;
    private String startTime;
    private String endTime;
    private Integer semYear;
    private String semTerm;
    private double similarity;
}
