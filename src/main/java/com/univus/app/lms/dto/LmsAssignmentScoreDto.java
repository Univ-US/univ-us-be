package com.univus.app.lms.dto;

import lombok.*;

/** PLM-003-01 과제별 점수 한 줄 (알고리즘 구현 #1: 68 / 미제출) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsAssignmentScoreDto {
    private Long assignmentId;            // LECTURE_ASSIGNMENT.LEC_ASN_ID
    private String title;                 // LECTURE_ASSIGNMENT.LEC_ASN_TITLE
    private Integer score;               // 채점 점수 (미제출/미채점이면 null)
    private String submissionStatusCode;  // NSB/SBM/GRD/RTN (제출 없으면 null)
    private String submissionStatusLabel; // 미제출/제출/채점완료 — service
    private boolean submitted;            // 제출 여부 (NSB 아님) — service
    private boolean scored;               // 채점 여부 (score != null) — service
}
