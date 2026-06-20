package com.univus.app.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.mapper.AdminMapper;
import com.univus.app.ai.dto.AiDto;
import com.univus.app.ai.dto.LectureVectorDto;
import com.univus.app.ai.dto.NoticeVectorDto;
import com.univus.app.ai.mapper.AiMapper;
import com.univus.app.ai.repository.LectureVectorRepository;
import com.univus.app.ai.repository.NoticeVectorRepository;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.weather.dto.WeatherDto;
import com.univus.app.weather.service.WeatherService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiMapper aiMapper;
    private final MemberMapper memberMapper;
    private final AdminMapper adminMapper;
    private final WeatherService weatherService;
    private final WebClient webClient;
    private final EmbeddingService embeddingService;
    private final NoticeVectorRepository noticeVectorRepository;
    private final LectureVectorRepository lectureVectorRepository;

    @Value("${univus.api.groq-api-key}")
    private String groqApiKey;

    @Override
    public void chat(AiDto aiDto, HttpServletResponse response) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        MemberDto member = memberMapper.findByMemberId(memberId);

        String memberName = member.getMemberName() != null ? member.getMemberName() : "사용자";
        String idLabel = "PROF".equals(member.getRole()) ? "교번" : "학번";
        String loginId = member.getLoginId() != null ? member.getLoginId() : "알 수 없음";
        String univName = member.getUnivName() != null ? member.getUnivName() : "알 수 없는 학교";
        String schoolPhone = member.getSchoolPhone() != null ? member.getSchoolPhone() : "알 수 없음";
        String homepage = member.getHomepage() != null ? member.getHomepage() : "알 수 없음";
        String systemPrompt = String.format(AiConstants.SYSTEM_PROMPT_TEMPLATE, memberName, idLabel, loginId, univName, schoolPhone, homepage);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        List<AiDto> history = aiMapper.selectRecentLogs(memberId);
        for (AiDto histLog : history) {
            messages.add(Map.of("role", "user", "content", histLog.getMessage() != null ? histLog.getMessage() : ""));
            messages.add(Map.of("role", "assistant", "content", histLog.getResponse() != null ? histLog.getResponse() : ""));
        }

        String userMessage = aiDto.getMessage() != null ? aiDto.getMessage() : "";

        StringBuilder fullResponse = new StringBuilder();
        setupSseHeaders(response);
        PrintWriter writer = response.getWriter();

        if (isVagueNoticeQuery(userMessage)) {
            String clarify = "📋 어떤 공지사항이 궁금하신가요?\n\n예를 들어 이렇게 물어보세요!\n- 최근 공지사항 알려줘\n- 학사 공지사항 뭐 있어?\n- 장학금 관련 공지 보여줘";
            writer.print("data:" + clarify + "\n\n");
            writer.flush();
            fullResponse.append(clarify);
        } else {
            String finalUserMessage = userMessage;
            if (isWeatherQuery(userMessage)) {
                String city = extractCity(userMessage);
                if (city == null) {
                    AdminDto.UniversityDto univ = adminMapper.selectUniversityById(member.getUnivId());
                    if (univ != null) city = extractCityFromAddress(univ.getAddress());
                }
                WeatherDto weather = weatherService.getWeatherByCityName(city != null ? city : "Seoul");
                if (weather != null) {
                    finalUserMessage = userMessage + String.format(
                            "\n\n[날씨 정보]\n도시: %s / 기온: %.1f°C / 날씨: %s",
                            weather.getCity(), weather.getTemp(), weather.getDescription());
                }
            }

            messages.add(Map.of("role", "user", "content", finalUserMessage));

            try {
                if (isNoticeQuery(userMessage)) {
                    injectNoticeContext(messages, member, userMessage);
                } else if (isLectureQuery(userMessage)) {
                    injectLectureContext(messages, member, userMessage);
                }
                streamResponse(messages, writer, fullResponse);
            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : "";
                log.error("Groq API 호출 실패 [message={}]: {}", userMessage, errMsg);
                String friendlyMsg;
                if (errMsg.startsWith("RATE_LIMIT")) {
                    friendlyMsg = "요청이 너무 많아요! 잠깐 기다렸다가 다시 말해줘요 ⏳";
                } else if (errMsg.startsWith("BAD_REQUEST")) {
                    friendlyMsg = "앗, 이 내용은 제가 답하기 어렵네요 🙈 다른 걸 물어봐 주세요!";
                } else {
                    friendlyMsg = "죄송해요, 잠시 문제가 생겼어요. 다시 시도해 주세요. 🙏";
                }
                writer.print("data:" + friendlyMsg + "\n\n");
                writer.flush();
            }
        }

        if (!fullResponse.isEmpty()) {
            aiDto.setMemberId(memberId);
            aiDto.setResponse(fullResponse.toString());
            aiMapper.insertAiLog(aiDto);
        }
    }

    private boolean isVagueNoticeQuery(String message) {
        String trimmed = message.trim();
        return AiConstants.VAGUE_NOTICE_QUERIES.stream().anyMatch(trimmed::equals);
    }

    private boolean isNoticeQuery(String message) {
        return AiConstants.NOTICE_KEYWORDS.stream().anyMatch(message::contains);
    }

    private boolean isLectureQuery(String message) {
        return AiConstants.LECTURE_KEYWORDS.stream().anyMatch(message::contains);
    }

    private boolean isWeatherQuery(String message) {
        return AiConstants.WEATHER_KEYWORDS.stream().anyMatch(message::contains);
    }

    private void injectNoticeContext(List<Map<String, Object>> messages, MemberDto member, String userMessage) throws Exception {
        String noticesJson;
        try {
            float[] queryEmbedding = embeddingService.embed(userMessage);
            List<NoticeVectorDto> similar = noticeVectorRepository.findSimilar(
                    member.getUnivId(), queryEmbedding, member.getRole(), 5);
            noticesJson = similar.isEmpty() ? "[]" : vectorNoticesToJson(similar);
        } catch (Exception e) {
            log.warn("벡터 검색 실패, 전체 공지 목록으로 폴백: {}", e.getMessage());
            String filterRole = "ADM".equals(member.getRole()) ? null : member.getRole();
            List<AdminDto.NoticeListDto> notices = adminMapper.selectNoticeList(member.getUnivId(), filterRole);
            noticesJson = notices == null || notices.isEmpty() ? "[]" : noticesToJson(notices);
        }

        Map<String, Object> fakeAssistant = new HashMap<>();
        fakeAssistant.put("role", "assistant");
        fakeAssistant.put("content", "");
        fakeAssistant.put("tool_calls", List.of(Map.of(
                "id", "direct_call",
                "type", "function",
                "function", Map.of("name", "get_notices", "arguments", "{}")
        )));
        messages.add(fakeAssistant);
        messages.add(Map.of(
                "role", "tool",
                "tool_call_id", "direct_call",
                "content", noticesJson
        ));
    }

    private void injectLectureContext(List<Map<String, Object>> messages, MemberDto member, String userMessage) throws Exception {
        String lecturesJson;
        try {
            float[] queryEmbedding = embeddingService.embed(userMessage);
            List<LectureVectorDto> similar = lectureVectorRepository.findSimilar(member.getUnivId(), queryEmbedding, 5);
            lecturesJson = similar.isEmpty() ? "[]" : vectorLecturesToJson(similar);
        } catch (Exception e) {
            log.warn("강의 벡터 검색 실패, 전체 목록으로 폴백: {}", e.getMessage());
            List<AdminDto.LectureAssignListDto> lectures = adminMapper.selectLectureAssignList(member.getUnivId(), null);
            lecturesJson = lectures == null || lectures.isEmpty() ? "[]" : lecturesToJson(lectures);
        }

        Map<String, Object> fakeAssistant = new HashMap<>();
        fakeAssistant.put("role", "assistant");
        fakeAssistant.put("content", "");
        fakeAssistant.put("tool_calls", List.of(Map.of(
                "id", "direct_call_lec",
                "type", "function",
                "function", Map.of("name", "get_lectures", "arguments", "{}")
        )));
        messages.add(fakeAssistant);
        messages.add(Map.of(
                "role", "tool",
                "tool_call_id", "direct_call_lec",
                "content", lecturesJson
        ));
    }

    private String vectorLecturesToJson(List<LectureVectorDto> lectures) throws Exception {
        List<String> names = new ArrayList<>();
        for (LectureVectorDto l : lectures) {
            names.add(l.getLecCodName());
        }
        return objectMapper.writeValueAsString(names);
    }

    private String lecturesToJson(List<AdminDto.LectureAssignListDto> lectures) throws Exception {
        List<String> names = new ArrayList<>();
        for (AdminDto.LectureAssignListDto l : lectures) {
            names.add(l.getLecCodName());
        }
        return objectMapper.writeValueAsString(names);
    }

    private String vectorNoticesToJson(List<NoticeVectorDto> notices) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        for (NoticeVectorDto n : notices) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", n.getTitle());
            m.put("target", n.getTarget());
            m.put("postedAt", n.getPostedAt() != null ? n.getPostedAt().toString() : null);
            list.add(m);
        }
        return objectMapper.writeValueAsString(list);
    }

    private void streamResponse(List<Map<String, Object>> messages, PrintWriter writer, StringBuilder fullResponse) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("messages", messages);
        body.put("max_tokens", 2000);
        body.put("stream", true);

        webClient.post()
                .uri(AiConstants.GROQ_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.value() == 429,
                        resp -> resp.bodyToMono(String.class).map(b -> new RuntimeException("RATE_LIMIT:" + b)))
                .onStatus(status -> status.value() == 400,
                        resp -> resp.bodyToMono(String.class).map(b -> new RuntimeException("BAD_REQUEST:" + b)))
                .onStatus(org.springframework.http.HttpStatusCode::isError,
                        resp -> resp.bodyToMono(String.class)
                                .map(b -> new RuntimeException("GROQ_ERROR:" + resp.statusCode().value() + ":" + b)))
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
    }

    private void setupSseHeaders(HttpServletResponse response) {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private String extractCity(String message) {
        for (String keyword : List.of("날씨", "기온", "온도")) {
            int idx = message.indexOf(keyword);
            if (idx > 0) {
                String before = message.substring(0, idx).trim();
                String[] words = before.split("\\s+");
                if (words.length > 0) {
                    String candidate = words[words.length - 1];
                    if (!AiConstants.NON_CITY_WORDS.contains(candidate) && candidate.length() >= 2) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private String extractCityFromAddress(String address) {
        if (address == null || address.isBlank()) return null;
        String[] parts = address.split("\\s+");
        if (parts.length == 0) return null;
        String first = parts[0];
        if (first.contains("특별시") || first.contains("광역시") || first.contains("특별자치시")) {
            return first.replaceAll("(특별자치시|특별시|광역시)", "");
        }
        if (first.endsWith("도") && parts.length > 1) {
            return parts[1].replaceAll("[시군]$", "");
        }
        return first;
    }

    private String noticesToJson(List<AdminDto.NoticeListDto> notices) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AdminDto.NoticeListDto n : notices) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", n.getTitle());
            m.put("target", n.getTarget());
            m.put("memberName", n.getMemberName());
            m.put("postedAt", n.getPostedAt() != null ? n.getPostedAt().toString() : null);
            list.add(m);
        }
        return objectMapper.writeValueAsString(list);
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
