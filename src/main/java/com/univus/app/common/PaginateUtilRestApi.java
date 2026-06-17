package com.univus.app.common;

import java.util.List;

/**
 * REST(JSON) 서버 페이지네이션 공통 유틸 (MyBatis OFFSET/FETCH 기반).
 *
 * <p>기존 {@link PaginateUtil}은 JSP/MVC용으로 페이지네이션 <b>HTML 마크업</b>(&lt;a&gt; 태그 문자열)을
 * 생성한다 → JSON REST API에는 부적합. 본 유틸은 JSON API용으로 <b>페이지 계산 + 응답 빌드</b>만 담당한다.
 *
 * <p>사용 흐름(컨트롤러/서비스):
 * <pre>
 *   int page   = PaginateUtilRestApi.normalizePage(reqPage);
 *   int size   = PaginateUtilRestApi.normalizeSize(reqSize);
 *   int offset = PaginateUtilRestApi.offset(page, size);
 *   List&lt;T&gt; rows = mapper.selectPaged(..., offset, size);   // OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY (Oracle 12c+)
 *   long total   = mapper.countXxx(...);                        // 동일 WHERE 조건의 별도 count 쿼리
 *   return PaginateUtilRestApi.of(rows, total, page, size);        // PaginateUtilRestApiRes&lt;T&gt;
 * </pre>
 *
 * <p>page = 0-based. 상태 없는 정적 유틸.
 */
public final class PaginateUtilRestApi {

    /** 기본 페이지 크기 (size 미지정 시) */
    public static final int DEFAULT_SIZE = 10;
    /** 최대 페이지 크기 (과도한 size 요청으로 전체 덤프 되는 것 방지) */
    public static final int MAX_SIZE = 100;

    private PaginateUtilRestApi() {
    }

    /** page 보정: null/음수 → 0 */
    public static int normalizePage(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }

    /** size 보정: null/0이하 → 기본값(DEFAULT_SIZE), MAX_SIZE 초과 → MAX_SIZE로 캡 */
    public static int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /** 매퍼 OFFSET 값 = page(0-based) * size */
    public static int offset(int page, int size) {
        return Math.max(0, page) * Math.max(0, size);
    }

    /** content + 전체 개수로 PaginateUtilRestApiRes 빌드 */
    public static <T> PaginateUtilRestApiRes<T> of(List<T> content, long totalElements, int page, int size) {
        return new PaginateUtilRestApiRes<>(content, page, size, totalElements);
    }
}
