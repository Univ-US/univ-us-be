package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuMaterialsDto;
import com.univus.app.lms.service.LmsStuMaterialsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lms/student/materials")
@RequiredArgsConstructor
public class LmsStuMaterialsController {

    private final LmsStuMaterialsService lmsStuMaterialsService;

    @GetMapping
    public ResponseEntity<List<LmsStuMaterialsDto.SemesterMaterialsResDto>> requestGetMaterials(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuMaterialsService.getMaterials(memberId));
    }

    @GetMapping("/attachments/{attachmentId}/file")
    public ResponseEntity<?> requestDownloadAttachment(
            Authentication authentication,
            @PathVariable Long attachmentId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return lmsStuMaterialsService.downloadAttachment(memberId, attachmentId);
    }
}
