package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuAssignmentsDto;
import com.univus.app.lms.service.LmsStuAssignmentsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lms/student/assignments")
@RequiredArgsConstructor
public class LmsStuAssignmentsController {

    private final LmsStuAssignmentsService lmsStuAssignmentsService;

    @GetMapping
    public ResponseEntity<LmsStuAssignmentsDto.AssignmentsResultResDto> requestGetAssignments(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuAssignmentsService.getAssignments(memberId));
    }

    @GetMapping("/submittable")
    public ResponseEntity<List<LmsStuAssignmentsDto.SubmittableAssignmentResDto>> requestGetSubmittableAssignments(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuAssignmentsService.getSubmittableAssignments(memberId));
    }

    @PostMapping(value = "/{assignmentId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> requestSubmitAssignment(
            Authentication authentication,
            @PathVariable Long assignmentId,
            @Valid @ModelAttribute LmsStuAssignmentsDto.SubmitReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsStuAssignmentsService.submitAssignment(memberId, assignmentId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/submissions/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> requestUpdateSubmission(
            Authentication authentication,
            @PathVariable Long submissionId,
            @Valid @ModelAttribute LmsStuAssignmentsDto.UpdateSubmissionReqDto request) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsStuAssignmentsService.updateSubmission(memberId, submissionId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<?> requestDownloadSubmissionFile(
            Authentication authentication,
            @PathVariable Long submissionId) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return lmsStuAssignmentsService.downloadSubmissionFile(memberId, submissionId);
    }
}
