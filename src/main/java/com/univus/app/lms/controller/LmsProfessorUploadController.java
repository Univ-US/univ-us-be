package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsUploadDto;
import com.univus.app.lms.service.LmsProfessorUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* PLM-005 / PLM-005-01 교수 강의 자료 업로드 Controller */
@RestController
@RequestMapping("/api/lms/professor/uploads")
@RequiredArgsConstructor
public class LmsProfessorUploadController {

    private final LmsProfessorUploadService lmsProfessorUploadService;

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    // GET /api/lms/professor/uploads/lectures
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsUploadDto.Lecture>> requestGetLectures(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfessorUploadService.getLectures(memberId));
    }

    /** PLM-005 자료 목록 (담당 강의 전체 자료, 최신순 — 페이지네이션은 FE 클라이언트 슬라이싱) */
    // GET /api/lms/professor/uploads
    @GetMapping
    public ResponseEntity<List<LmsUploadDto.Material>> requestGetUploads(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfessorUploadService.getUploads(memberId));
    }

    /** PLM-005-01 자료 등록 (multipart: lecId·title 필수, content[에디터 HTML]·file 선택) → 생성 자료 반환 */
    // POST /api/lms/professor/uploads
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsUploadDto.Material> requestCreateUpload(
            Authentication authentication,
            @Valid @ModelAttribute LmsUploadDto.CreateRequest request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lmsProfessorUploadService.createUpload(memberId, request));
    }

    /** PLM-005 자료 수정 (multipart: title·content·file 선택 — 파일 미첨부 시 기존 첨부 유지) */
    // PUT /api/lms/professor/uploads/{uploadId}
    @PutMapping(value = "/{uploadId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsUploadDto.Material> requestUpdateUpload(
            Authentication authentication,
            @PathVariable Long uploadId,
            @Valid @ModelAttribute LmsUploadDto.UpdateRequest request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfessorUploadService.updateUpload(memberId, uploadId, request));
    }

    /** PLM-005 자료 삭제 (첨부 → 본체 물리 삭제) */
    // DELETE /api/lms/professor/uploads/{uploadId}
    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Void> requestDeleteUpload(
            Authentication authentication,
            @PathVariable Long uploadId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsProfessorUploadService.deleteUpload(memberId, uploadId);
        return ResponseEntity.noContent().build();
    }
}
