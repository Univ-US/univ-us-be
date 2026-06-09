package com.univus.app.lms.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

/** PLM-003 수강생 목록 한 행 (단순화: 상태 열 제거) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsStudentRowDto {
    private Long enrollmentId;        // LECTURE_STUDENT_ENROLLMENT.LEC_STD_ENR_ID
    private Long memberId;            // 학생 MEMBER_ID (상세 리포트 호출용)
    private String studentName;       // MEMBER.MEMBER_NAME
    private String studentNo;         // MEMBER.LOGIN_ID (학번)
    private int attendanceRate;       // 출석률(%) — service 계산
    private Integer submittedCount;   // 제출 과제 수
    private Integer totalAssignments; // 전체 과제 수 — service 세팅(강의 단위)
    private Integer averageScore;     // 평균 점수 (채점된 과제 평균, 없으면 null)

    // --- service 계산용 raw 집계 (응답 제외) ---
    @JsonIgnore private Integer presentCount;    // 출석(PRS) 횟수
    @JsonIgnore private Integer attendanceTotal; // 전체 출석 기록 수
}
