package com.univus.app.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginateUtilTest {

    private final PaginateUtil paginateUtil = new PaginateUtil();

    @Test
    @DisplayName("딱 나누어떨어지면 몫이 총 페이지 수")
    void pageCount_exactDivision() {
        assertEquals(5, paginateUtil.pageCount(50, 10));
    }

    @Test
    @DisplayName("나머지가 있으면 한 페이지 올림")
    void pageCount_withRemainder() {
        assertEquals(6, paginateUtil.pageCount(51, 10));
    }

    @Test
    @DisplayName("데이터가 0개면 0페이지")
    void pageCount_zeroData() {
        assertEquals(0, paginateUtil.pageCount(0, 10));
    }

    @Test
    @DisplayName("잘못된 입력(음수/0)은 0페이지")
    void pageCount_invalidInput() {
        assertEquals(0, paginateUtil.pageCount(-5, 10));
        assertEquals(0, paginateUtil.pageCount(10, 0));
    }
}