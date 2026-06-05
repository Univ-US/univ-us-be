package com.univus.app.ai.controller;

import com.univus.app.ai.dto.AiDto;
import com.univus.app.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody AiDto aiDto) {
        Map<String, Object> result = aiService.chat(aiDto);
        return ResponseEntity.ok(result);
    }
}