package com.univus.app.lms.dto;

import lombok.*;

/** PLM-003 상단 통계 카드 3개 (단순화: 저조 학생 카드 제거) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsStudentStatsSummaryDto {
    private int totalStudents;          // 전체 수강생 (수강 인원)
    private int averageAttendanceRate;  // 평균 출석률(%)
    private int averageSubmissionRate;  // 과제 제출률(%)
}
