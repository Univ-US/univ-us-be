package com.univus.app.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AlertFailTest {
    @Test
    void alwaysFails() {
        assertEquals(1, 2);   // 일부러 실패시켜 CI 실패 알림(A) 확인
    }
}