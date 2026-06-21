package com.univus.app.lms.controller;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuNoticeDto;
import com.univus.app.lms.service.LmsStuNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* SLM-009 학생 공지 Controller (수강 과목 선택 → 서버 페이지네이션) */
@RestController
@RequestMapping("/api/lms/student/notices")
@RequiredArgsConstructor
public class LmsStuNoticeController {

    private final LmsStuNoticeService lmsStuNoticeService;

    /** 수강 과목(강의) 드롭다운 — 년도/학기/과목 필터 소스 */
    // GET /api/lms/student/notices/lectures
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsStuNoticeDto.LectureResDto>> requestGetLectures(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuNoticeService.getLectures(memberId));
    }

    /** 선택 과목 공지 1페이지 — 서버 페이지네이션. page 0-based, size 기본 10 */
    // GET /api/lms/student/notices?lecId=&page=&size=
    @GetMapping
    public ResponseEntity<PaginateUtilRestApiRes<LmsStuNoticeDto.NoticeResDto>> requestGetNotices(
            Authentication authentication,
            @RequestParam("lecId") Long lecId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        return ResponseEntity.ok(lmsStuNoticeService.getNotices(memberId, lecId, p, s));
    }

    @GetMapping("/attachments/{attachmentId}/file")
    public ResponseEntity<?> requestDownloadAttachment(
            Authentication authentication,
            @PathVariable Long attachmentId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return lmsStuNoticeService.downloadAttachment(memberId, attachmentId);
    }
}
