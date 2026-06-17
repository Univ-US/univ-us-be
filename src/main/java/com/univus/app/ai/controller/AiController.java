package com.univus.app.ai.controller;

import com.univus.app.ai.dto.AiDto;
import com.univus.app.ai.service.AiService;
import com.univus.app.ai.service.LectureEmbeddingBatchService;
import com.univus.app.ai.service.NoticeEmbeddingBatchService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final NoticeEmbeddingBatchService noticeEmbeddingBatchService;
    private final LectureEmbeddingBatchService lectureEmbeddingBatchService;

    @PostMapping("/stream")
    public void chat(@RequestBody AiDto aiDto, HttpServletResponse response) throws Exception {
        aiService.chat(aiDto, response);
    }

    @PostMapping("/embed-notices/{univId}")
    public ResponseEntity<String> embedNotices(@PathVariable Long univId) {
        int count = noticeEmbeddingBatchService.embedAll(univId);
        return ResponseEntity.ok("임베딩 완료: " + count + "건");
    }

    @PostMapping("/embed-lectures/{univId}")
    public ResponseEntity<String> embedLectures(@PathVariable Long univId) {
        int count = lectureEmbeddingBatchService.embedAll(univId);
        return ResponseEntity.ok("강의 임베딩 완료: " + count + "건");
    }
}
