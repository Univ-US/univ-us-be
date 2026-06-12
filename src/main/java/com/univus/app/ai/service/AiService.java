package com.univus.app.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univus.app.ai.dto.AiDto;
import com.univus.app.ai.mapper.AiMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.PrintWriter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiMapper aiMapper;
    private final WebClient webClient;

    @Value("${univus.api.groq-api-key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String SYSTEM_PROMPT = "너는 univus 캠퍼스 ERP 서비스의 AI 챗봇'유니봇'이야. 캠퍼스 생활, 강의, 커뮤니티 관련 질문에 친절하게 답해줘.답변할 때 이모티콘을 적극적으로 사용해서 친근하게 답변해줘.";

    public void chat(AiDto aiDto, HttpServletResponse response) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        List<AiDto> history = aiMapper.selectRecentLogs(memberId);
        for (AiDto log : history) {
            messages.add(Map.of("role", "user", "content", log.getMessage() != null ? log.getMessage() : ""));
            messages.add(Map.of("role", "assistant", "content", log.getResponse() != null ? log.getResponse() : ""));
        }
        String userMessage = aiDto.getMessage() != null ? aiDto.getMessage() : "";
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("messages", messages);
        body.put("max_tokens", 1000);
        body.put("stream", true);

        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();
        StringBuilder fullResponse = new StringBuilder();

        webClient.post()
                .uri(GROQ_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .takeUntil(event -> "[DONE]".equals(event.data()))
                .filter(event -> !"[DONE]".equals(event.data()))
                .mapNotNull(event -> extractToken(event.data()))
                .filter(token -> !token.isEmpty())
                .doOnNext(token -> {
                    writer.print("data:" + token + "\n\n");
                    writer.flush();
                    fullResponse.append(token);
                })
                .blockLast();

        aiDto.setMemberId(memberId);
        aiDto.setResponse(fullResponse.toString());
        aiMapper.insertAiLog(aiDto);
    }

    private String extractToken(String json) {
        try {
            Map<?, ?> chunk = objectMapper.readValue(json, Map.class);
            List<?> choices = (List<?>) chunk.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> delta = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("delta");
                if (delta != null) {
                    String content = (String) delta.get("content");
                    return content != null ? content : "";
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
