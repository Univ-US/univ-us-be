package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuNoticeDto;
import com.univus.app.lms.service.LmsStuNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lms/student/notices")
@RequiredArgsConstructor
public class LmsStuNoticeController {

    private final LmsStuNoticeService lmsStuNoticeService;

    @GetMapping
    public ResponseEntity<List<LmsStuNoticeDto.NoticeResDto>> requestGetNotices(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuNoticeService.getNotices(memberId));
    }

    @GetMapping("/attachments/{attachmentId}/file")
    public ResponseEntity<?> requestDownloadAttachment(
            Authentication authentication,
            @PathVariable Long attachmentId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return lmsStuNoticeService.downloadAttachment(memberId, attachmentId);
    }
}
