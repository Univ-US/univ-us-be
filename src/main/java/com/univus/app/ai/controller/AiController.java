package com.univus.app.ai.controller;

import com.univus.app.ai.dto.AiDto;
import com.univus.app.ai.service.AiService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/stream")
    public void chat(@RequestBody AiDto aiDto, HttpServletResponse response) throws Exception {
        aiService.chat(aiDto, response);
    }
}
