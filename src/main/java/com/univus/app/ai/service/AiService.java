package com.univus.app.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.mapper.AdminMapper;
import com.univus.app.ai.dto.AiDto;
import com.univus.app.ai.mapper.AiMapper;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.weather.dto.WeatherDto;
import com.univus.app.weather.service.WeatherService;
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
    private final MemberMapper memberMapper;
    private final AdminMapper adminMapper;
    private final WeatherService weatherService;
    private final WebClient webClient;

    @Value("${univus.api.groq-api-key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String SYSTEM_PROMPT_TEMPLATE =
            "너는 univus 캠퍼스 ERP 서비스의 AI 챗봇 '유니봇'이야. 캠퍼스 생활, 강의, 커뮤니티 관련 질문에 친절하게 답해줘. " +
            "공지사항 목록을 받으면 반드시 모든 항목을 빠짐없이 번호 목록 형식으로 정리해서 안내해줘. " +
            "날씨 관련 질문을 받으면 [날씨 정보] 섹션의 데이터를 그대로 활용해서 답해줘. 날씨 데이터가 없으면 '날씨 정보를 가져오지 못했어요'라고 짧게 말해줘. " +
            "답변할 때 이모티콘을 적극적으로 사용해서 친근하게 답변해줘.\n\n" +
            "[사용자 정보]\n" +
            "- 이름: %s\n" +
            "- %s: %s\n\n" +
            "[서비스 정보 — 아래 사실만 그대로 답변하고 절대 추가 정보를 지어내지 마]\n" +
            "- 현재 서비스를 이용 중인 학교: %s\n" +
            "- 학교 대표번호: %s\n" +
            "- 학교 홈페이지: %s\n" +
            "- 이 서비스 개발팀: EARTH 개발팀 (학교와 완전히 무관한 외부 개발팀. 절대 학교 이름을 개발팀과 연결하지 말 것)";

    private static final List<String> VAGUE_NOTICE_QUERIES = List.of("공지사항", "공지", "알림", "학교 소식");

    private static final List<String> NOTICE_KEYWORDS = List.of("공지", "알림", "학사 일정", "학교 소식", "공지사항");

    private static final List<String> WEATHER_KEYWORDS = List.of("날씨", "기온", "온도", "비 와", "눈 와", "맑아", "흐려");

    private static final Set<String> NON_CITY_WORDS = Set.of(
            "오늘", "내일", "어제", "지금", "현재", "좀", "이번", "요즘", "최근", "그", "이", "저", "어떤",
            "우리", "여기", "학교", "캠퍼스", "주말", "밖", "실외", "실내", "이번주", "다음주");

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
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, memberName, idLabel, loginId, univName, schoolPhone, homepage);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        List<AiDto> history = aiMapper.selectRecentLogs(memberId);
        for (AiDto log : history) {
            messages.add(Map.of("role", "user", "content", log.getMessage() != null ? log.getMessage() : ""));
            messages.add(Map.of("role", "assistant", "content", log.getResponse() != null ? log.getResponse() : ""));
        }
        String userMessage = aiDto.getMessage() != null ? aiDto.getMessage() : "";

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
                        "\n\n[현재 날씨 데이터 — 이 정보를 그대로 사용해서 답해줘]\n도시: %s / 기온: %.1f°C / 날씨: %s",
                        weather.getCity(), weather.getTemp(), weather.getDescription());
            }
        }

        messages.add(Map.of("role", "user", "content", finalUserMessage));

        StringBuilder fullResponse = new StringBuilder();

        setupSseHeaders(response);
        PrintWriter writer = response.getWriter();

        if (isVagueNoticeQuery(userMessage)) {
            String clarify = "📋 어떤 공지사항이 궁금하신가요?\n\n예를 들어 이렇게 물어보세요!\n- 최근 공지사항 알려줘\n- 학사 공지사항 뭐 있어?\n- 장학금 관련 공지 보여줘";
            writer.print("data:" + clarify + "\n\n");
            writer.flush();
            fullResponse.append(clarify);
        } else {
            try {
                if (isNoticeQuery(userMessage)) {
                    injectNoticeContext(messages, member);
                }
                streamResponse(messages, writer, fullResponse);
            } catch (Exception e) {
                writer.print("data:죄송해요, 잠시 문제가 생겼어요. 다시 시도해 주세요. 🙏\n\n");
                writer.flush();
            }
        }

        // 정상 응답이 있을 때만 DB에 저장
        if (!fullResponse.isEmpty()) {
            aiDto.setMemberId(memberId);
            aiDto.setResponse(fullResponse.toString());
            aiMapper.insertAiLog(aiDto);
        }
    }

    private boolean isWeatherQuery(String message) {
        return WEATHER_KEYWORDS.stream().anyMatch(message::contains);
    }

    private String extractCityFromAddress(String address) {
        if (address == null || address.isBlank()) return null;
        String[] parts = address.split("\\s+");
        if (parts.length == 0) return null;

        String first = parts[0];
        // 서울특별시, 부산광역시, 세종특별자치시 등
        if (first.contains("특별시") || first.contains("광역시") || first.contains("특별자치시")) {
            return first.replaceAll("(특별자치시|특별시|광역시)", "");
        }
        // 강원도, 경기도 등 — 두 번째 단어가 도시명
        if (first.endsWith("도") && parts.length > 1) {
            return parts[1].replaceAll("[시군]$", "");
        }
        return first;
    }

    private String extractCity(String message) {
        for (String keyword : List.of("날씨", "기온", "온도")) {
            int idx = message.indexOf(keyword);
            if (idx > 0) {
                String before = message.substring(0, idx).trim();
                String[] words = before.split("\\s+");
                if (words.length > 0) {
                    String candidate = words[words.length - 1];
                    if (!NON_CITY_WORDS.contains(candidate) && candidate.length() >= 2) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private void setupSseHeaders(HttpServletResponse response) {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private boolean isVagueNoticeQuery(String message) {
        String trimmed = message.trim();
        return VAGUE_NOTICE_QUERIES.stream().anyMatch(trimmed::equals);
    }

    private boolean isNoticeQuery(String message) {
        return NOTICE_KEYWORDS.stream().anyMatch(message::contains);
    }

    private void injectNoticeContext(List<Map<String, Object>> messages, MemberDto member) throws Exception {
        String filterRole = "ADM".equals(member.getRole()) ? null : member.getRole();
        List<AdminDto.NoticeListDto> notices = adminMapper.selectNoticeList(member.getUnivId(), filterRole);
        String noticesJson = noticesToJson(notices);

        // tool 호출 없이 직접 context로 주입 (fake tool call 구조 유지)
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

    private void streamResponse(List<Map<String, Object>> messages, PrintWriter writer, StringBuilder fullResponse) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("messages", messages);
        body.put("max_tokens", 2000);
        body.put("stream", true);

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
