package com.univus.app.lms.controller;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsProfAssignmentDto;
import com.univus.app.lms.service.LmsProfAssignmentService;
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

/* PLM-006 교수 과제 관리 Controller — 과제 CRUD + 첨부(다중) + 제출/채점/수강생 집계 */
@RestController
@RequestMapping("/api/lms/professor/assignments")
@RequiredArgsConstructor
public class LmsProfAssignmentController {

    private final LmsProfAssignmentService lmsProfAssignmentService;

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    // GET /api/lms/professor/assignments/lectures
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsProfAssignmentDto.LectureResDto>> requestGetLectures(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfAssignmentService.getLectures(memberId));
    }

    /** 선택 과목 1개의 과제 1페이지 — 서버 페이지네이션(과목 드롭다운은 /lectures 재사용). page 0-based, size 기본 10 */
    // GET /api/lms/professor/assignments?lecId=&page=&size=
    @GetMapping
    public ResponseEntity<PaginateUtilRestApiRes<LmsProfAssignmentDto.AssignmentResDto>> requestGetCourseAssignments(
            Authentication authentication,
            @RequestParam("lecId") Long lecId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        Long memberId = (Long) authentication.getPrincipal();
        int p = page == null ? 0 : page;
        int s = size == null ? 10 : size;
        return ResponseEntity.ok(lmsProfAssignmentService.getCourseAssignments(memberId, lecId, p, s));
    }

    /** 과제 등록 (multipart: lecId·title·dueDate 필수, description·files 선택) → 생성 과제 반환 */
    // POST /api/lms/professor/assignments
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsProfAssignmentDto.AssignmentResDto> requestCreateAssignment(
            Authentication authentication,
            @Valid @ModelAttribute LmsProfAssignmentDto.CreateReqDto request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lmsProfAssignmentService.createAssignment(memberId, request));
    }

    /** 과제 수정 (multipart: title·dueDate·description + files 추가 / removeAttachmentIds 제거) */
    // PUT /api/lms/professor/assignments/{assignmentId}
    @PutMapping(value = "/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsProfAssignmentDto.AssignmentResDto> requestUpdateAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @Valid @ModelAttribute LmsProfAssignmentDto.UpdateReqDto request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfAssignmentService.updateAssignment(memberId, assignmentId, request));
    }

    /** 과제 삭제 (첨부 → 본체 물리 삭제) */
    // DELETE /api/lms/professor/assignments/{assignmentId}
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> requestDeleteAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        Long memberId = (Long) authentication.getPrincipal();
        lmsProfAssignmentService.deleteAssignment(memberId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
