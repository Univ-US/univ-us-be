package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuAssignmentsDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LmsStuAssignmentsService {

    LmsStuAssignmentsDto.AssignmentsResultResDto getAssignments(Long memberId);

    List<LmsStuAssignmentsDto.SubmittableAssignmentResDto> getSubmittableAssignments(Long memberId);

    void submitAssignment(Long memberId, Long assignmentId, LmsStuAssignmentsDto.SubmitReqDto request);

    void updateSubmission(Long memberId, Long submissionId, LmsStuAssignmentsDto.UpdateSubmissionReqDto request);

    ResponseEntity<?> downloadSubmissionFile(Long memberId, Long submissionId);
}
