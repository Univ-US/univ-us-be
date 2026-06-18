package com.univus.app.inquiry.controller;

import com.univus.app.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries/attachments")
@RequiredArgsConstructor
public class InquiryAttachmentController {

    private final InquiryService inquiryService;

    @GetMapping("/{attachmentId}")
    public ResponseEntity<?> download(
            Authentication authentication,
            @PathVariable Long attachmentId
    ) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return inquiryService.downloadAttachment(memberId, attachmentId);
    }
}
