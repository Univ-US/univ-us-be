package com.univus; // 👈 소속 패키지 부활! (경로에 따라 com.univus.app 일 수도 있음)

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource
class UnivusApplicationTests {

    @Test
    void contextLoads() {
    }
}