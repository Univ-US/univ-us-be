package com.univus.app.lms.dto;

import lombok.*;

/** PLM-003 수강생 목록 페이지네이션 메타 (검색·제출필터 적용 후 전체 기준) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsPageInfoDto {
    private int page;          // 현재 페이지 (0-based)
    private int size;          // 페이지 크기
    private int totalElements; // 필터 적용된 전체 건수
    private int totalPages;    // 전체 페이지 수 (수강생 0~size명이어도 최소 1)
}
