package com.univus.app.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * ci-cd-dev "불합격" 검증용 일부러 실패하는 테스트.
 * (non-feat 브랜치도 실패 시 병합이 차단되는지 확인)
 * 검증 후 이 파일과 test/ci-fail 브랜치는 삭제한다.
 */
class CiFailTest {

    @Test
    void alwaysFails() {
        assertTrue(false);   // 일부러 실패
    }
}
