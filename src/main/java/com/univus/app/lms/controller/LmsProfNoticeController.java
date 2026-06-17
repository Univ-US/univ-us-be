package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsProfNoticeDto;
import com.univus.app.lms.service.LmsProfNoticeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* PLM-007 교수 공지사항 관리 Controller (LECTURE_ANNOUNCEMENT). 인증=PROF 가드(SecurityConfig /api/lms/professor/**) */
@RestController
@RequestMapping("/api/lms/professor/notices")
@RequiredArgsConstructor
public class LmsProfNoticeController {

    private final LmsProfNoticeService lmsProfNoticeService;

    /** 공지 작성 대상 = 교수 담당 강의 드롭다운 */
    // GET /api/lms/professor/notices/lectures
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsProfNoticeDto.LectureResDto>> requestGetLectures(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfNoticeService.getLectures(memberId));
    }

    /** 선택 강의의 공지 목록 (최신순) */
    // GET /api/lms/professor/notices?lecId=
    @GetMapping
    public ResponseEntity<List<LmsProfNoticeDto.NoticeResDto>> requestGetNotices(
            Authentication authentication,
            @RequestParam("lecId") Long lecId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfNoticeService.getNotices(memberId, lecId));
    }

    /** 공지 작성 (multipart: lecId·title 필수, content[에디터 HTML]·files 선택) → 생성 공지 반환 */
    // POST /api/lms/professor/notices
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsProfNoticeDto.NoticeResDto> requestCreateNotice(
            Authentication authentication,
            @Valid @ModelAttribute LmsProfNoticeDto.CreateReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lmsProfNoticeService.createNotice(memberId, request));
    }

    /** 공지 수정 (multipart: title·content·files·removeAttachmentIds — 강의 변경 불가) */
    // PUT /api/lms/professor/notices/{noticeId}
    @PutMapping(value = "/{noticeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsProfNoticeDto.NoticeResDto> requestUpdateNotice(
            Authentication authentication,
            @PathVariable Long noticeId,
            @Valid @ModelAttribute LmsProfNoticeDto.UpdateReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfNoticeService.updateNotice(memberId, noticeId, request));
    }

    /** 공지 삭제 (첨부 → 본체 물리 삭제) */
    // DELETE /api/lms/professor/notices/{noticeId}
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> requestDeleteNotice(
            Authentication authentication,
            @PathVariable Long noticeId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsProfNoticeService.deleteNotice(memberId, noticeId);
        return ResponseEntity.noContent().build();
    }

    /** 공지 첨부 다운로드 (인증 blob — 본인 강의 첨부만, 없으면 404) */
    // GET /api/lms/professor/notices/attachments/{attachmentId}/file
    @GetMapping("/attachments/{attachmentId}/file")
    public ResponseEntity<?> requestDownloadAttachment(
            Authentication authentication,
            @PathVariable Long attachmentId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return lmsProfNoticeService.downloadAttachment(memberId, attachmentId);
    }
}
