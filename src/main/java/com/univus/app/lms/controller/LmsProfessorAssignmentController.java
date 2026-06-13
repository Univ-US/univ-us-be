package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsAssignmentDto;
import com.univus.app.lms.service.LmsProfessorAssignmentService;
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

/* PLM-006 교수 과제 관리 Controller — 과제 CRUD + 첨부(다중) + 제출/채점/수강생 집계 */
@RestController
@RequestMapping("/api/lms/professor/assignments")
@RequiredArgsConstructor
public class LmsProfessorAssignmentController {

    private final LmsProfessorAssignmentService lmsProfessorAssignmentService;

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    // GET /api/lms/professor/assignments/lectures
    @GetMapping("/lectures")
    public ResponseEntity<List<LmsAssignmentDto.Lecture>> requestGetLectures(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorAssignmentService.getLectures(memberId));
    }

    /** 과제 목록 (담당 강의 전체 과제 + 집계 + 첨부, 학기/그룹핑은 FE 클라이언트) */
    // GET /api/lms/professor/assignments
    @GetMapping
    public ResponseEntity<List<LmsAssignmentDto.Assignment>> requestGetAssignments(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorAssignmentService.getAssignments(memberId));
    }

    /** 과제 등록 (multipart: lecId·title·dueDate 필수, description·files 선택) → 생성 과제 반환 */
    // POST /api/lms/professor/assignments
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsAssignmentDto.Assignment> requestCreateAssignment(
            Authentication authentication,
            @Valid @ModelAttribute LmsAssignmentDto.CreateRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lmsProfessorAssignmentService.createAssignment(memberId, request));
    }

    /** 과제 수정 (multipart: title·dueDate·description + files 추가 / removeAttachmentIds 제거) */
    // PUT /api/lms/professor/assignments/{assignmentId}
    @PutMapping(value = "/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsAssignmentDto.Assignment> requestUpdateAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @Valid @ModelAttribute LmsAssignmentDto.UpdateRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorAssignmentService.updateAssignment(memberId, assignmentId, request));
    }

    /** 과제 삭제 (첨부 → 본체 물리 삭제) */
    // DELETE /api/lms/professor/assignments/{assignmentId}
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> requestDeleteAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId) {
        Long memberId = (Long) authentication.getPrincipal();
        lmsProfessorAssignmentService.deleteAssignment(memberId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
