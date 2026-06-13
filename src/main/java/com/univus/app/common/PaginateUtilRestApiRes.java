package com.univus.app.common;

import java.util.List;
import lombok.Getter;

/**
 * REST(JSON) 서버 페이지네이션 공통 응답 래퍼.
 * MyBatis 기반(Spring Data Page 미사용) 목록 API의 표준 페이지 응답 형태.
 * 목록 컨트롤러는 {@code List<T>} 대신 {@code PaginateUtilRestApiRes<T>}를 반환한다.
 *
 * <p>page = 0-based. totalPages/first/last는 생성 시 자동 계산.
 * FE는 content + page/size/totalElements/totalPages로 페이저를 렌더한다.
 */
@Getter
public class PaginateUtilRestApiRes<T> {
    private final List<T> content;     // 현재 페이지 항목
    private final int page;            // 현재 페이지 번호 (0-based)
    private final int size;            // 페이지당 항목 수
    private final long totalElements;  // 전체 항목 수(필터 적용 후)
    private final int totalPages;      // 전체 페이지 수 (데이터 0이면 0)
    private final boolean first;       // 첫 페이지 여부
    private final boolean last;        // 마지막 페이지 여부

    public PaginateUtilRestApiRes(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
        this.first = page <= 0;
        this.last = this.totalPages == 0 || page >= this.totalPages - 1;
    }
}
