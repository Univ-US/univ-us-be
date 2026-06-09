package com.univus.app.lms.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.List;

/** PLM-003-01 수강생 상세 리포트 모달 (단순화: 상태 태그 제거, 출결=출석/지각/결석) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsStudentReportDto {
    private Long memberId;
    private String studentName;            // MEMBER.MEMBER_NAME
    private String studentNo;              // MEMBER.LOGIN_ID
    private String lectureName;            // LECTURE.LEC_NAME

    // 상단 요약 카드 3개 (출석률 / 과제 제출 / 평균 점수)
    private int attendanceRate;            // service 계산
    private Integer submittedCount;        // 제출 과제 수
    private Integer totalAssignments;      // 전체 과제 수 — service 세팅
    private Integer averageScore;          // 평균 점수 (없으면 null)

    // 출결 요약 (출석/지각/결석)
    private Integer attendancePresent;
    private Integer attendanceLate;
    private Integer attendanceAbsent;
    private Integer attendanceTotal;       // 전체 기록 수 (출석률 분모)

    // 과제별 점수
    private List<LmsAssignmentScoreDto> assignmentScores;

    // 내부용 (응답 제외) — 과제별 점수 조회 시 사용
    @JsonIgnore private Long studentLmsPrfId;
}
