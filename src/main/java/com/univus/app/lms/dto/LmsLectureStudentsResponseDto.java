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
    private LmsStudentStatsSummaryDto summary; // 통계 3카드 (검색 전체 기준)
    private List<LmsStudentRowDto> students;   // 수강생 목록 (현재 페이지)
    private LmsPageInfoDto pagination;         // 페이지네이션 메타 (검색·필터 적용 후 전체 기준)
}
