package com.univus.app.lms.dto;

import lombok.*;

/** 학기 드롭다운 (PLM-003 상단 "2026년 1학기") */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsSemesterResponseDto {
    private Long semId;          // SEMESTERS.SEM_ID
    private Integer year;        // SEMESTERS.SEM_YEAR
    private String termCode;     // SEMESTERS.SEM_TERM (SemesterCode: SM1/SM2/...)
    private String termLabel;    // SemesterCode.label ("1학기") — service에서 세팅
    private String displayName;  // "2026년 1학기" — service에서 세팅
}
