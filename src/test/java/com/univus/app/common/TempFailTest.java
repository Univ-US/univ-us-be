package com.univus.app.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TempFailTest {
    @Test
    void tempFail() {
        assertTrue(false);   // 일부러 실패
    }
}