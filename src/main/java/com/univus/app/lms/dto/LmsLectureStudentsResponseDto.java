package com.univus.app.lms.dto;

import lombok.*;

import java.util.List;

/** PLM-003 응답: 강의 헤더 + 통계 카드(3개) + 수강생 목록 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsLectureStudentsResponseDto {
    private LmsLectureDto lecture;             // 강의 헤더(이름·학기)
    private LmsStudentStatsSummaryDto summary; // 통계 3카드
    private List<LmsStudentRowDto> students;   // 수강생 목록
}
