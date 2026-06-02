package com.univus.app.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TempFailTest {
    @Test
    void alwaysFails() {
        assertTrue(false);   // 일부러 실패 (B 검증용)
    }
}