package com.univus.app.ai.service;

import com.univus.app.ai.dto.AiDto;
import com.univus.app.ai.mapper.AiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiMapper aiMapper;
    private final RestTemplate restTemplate;

    @Value("${univus.api.groq-api-key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String SYSTEM_PROMPT = "너는 univus 캠퍼스 ERP 서비스의 AI 챗봇이야. 캠퍼스 생활, 강의, 커뮤니티 관련 질문에 친절하게 답해줘.";

    public Map<String, Object> chat(AiDto aiDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", aiDto.getMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("messages", messages);
        body.put("max_tokens", 1000);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                GROQ_URL, HttpMethod.POST, request, Map.class);

        String answer = "";
        try {
            List<Map> choices = (List<Map>) response.getBody().get("choices");
            Map message = (Map) choices.get(0).get("message");
            answer = (String) message.get("content");
        } catch (Exception e) {
            answer = "죄송해요, 답변을 가져오지 못했어요.";
        }

        aiDto.setMemberId(1L);
        aiDto.setResponse(answer);
        aiMapper.insertAiLog(aiDto);

        return Map.of("response", answer);
    }
}