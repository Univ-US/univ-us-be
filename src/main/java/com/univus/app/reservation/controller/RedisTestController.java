package com.univus.app.reservation.controller; 

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/ping")
    public String testRedis() {
        redisTemplate.opsForValue().set("univus_test", "서버 연결 대성공! 🚀");

        String result = (String) redisTemplate.opsForValue().get("univus_test");

        return "Redis 응답: " + result;
    }
}