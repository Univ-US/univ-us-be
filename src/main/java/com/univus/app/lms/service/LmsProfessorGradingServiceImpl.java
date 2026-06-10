package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.lms.dto.LmsGradingDto;
import com.univus.app.lms.dto.LmsSemesterResponseDto;
import com.univus.app.lms.mapper.LmsProfessorGradingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LmsProfessorGradingServiceImpl implements LmsProfessorGradingService {

    private final LmsProfessorGradingMapper gradingMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    // TODO maxScore: LECTURE_ASSIGNMENT에 만점 컬럼이 없어 100 고정(잠정). 추후 재검토(컬럼 추가 vs 고정).
    private static final int DEFAULT_MAX_SCORE = 100;

    private static final String SUBMITTED_NONE = "NSB"; // 미제출 상태코드
    private static final String UPLOAD_WEB_PREFIX = "/uploads"; // ORG_URL 웹 접두어 (프로필 이미지와 동일 관례)
    private static final String FILE_DOWNLOAD_PATH = "/api/lms/professor/grading/submissions/%d/file";

    /* PLM-004 채점 개요 */
    @Override
    @Transactional(readOnly = true)
    public LmsGradingDto.Overview getOverview(Long memberId, Long semesterId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        LmsSemesterResponseDto semester = gradingMapper.findSemester(semesterId);
        Long semId = semester != null ? semester.getSemId() : semesterId;

        List<LmsGradingDto.AssignmentRow> rows = gradingMapper.selectOverviewAssignments(professorLmsPrfId, semId);

        List<LmsGradingDto.AssignmentRow> ungraded = new ArrayList<>();
        List<LmsGradingDto.AssignmentRow> graded = new ArrayList<>();
        Map<String, Integer> ungradedByCourse = new LinkedHashMap<>();
        int totalUngraded = 0;

        for (LmsGradingDto.AssignmentRow row : rows) {
            int ungradedCount = Math.max(0, row.getSubmittedCount() - row.getGradedCount());
            row.setUngradedCount(ungradedCount);
            row.setMaxScore(DEFAULT_MAX_SCORE);
            if (ungradedCount > 0) {
                ungraded.add(row);
                totalUngraded += ungradedCount;
                ungradedByCourse.merge(row.getCourseName(), ungradedCount, Integer::sum);
            } else {
                graded.add(row);
            }
        }

        List<LmsGradingDto.CourseCount> byCourse = new ArrayList<>();
        ungradedByCourse.forEach((course, count) ->
                byCourse.add(LmsGradingDto.CourseCount.builder().courseName(course).count(count).build()));

        return LmsGradingDto.Overview.builder()
                .year(semester != null ? semester.getYear() : null)
                .termCode(semester != null ? semester.getTermCode() : null)
                .totalUngraded(totalUngraded)
                .byCourse(byCourse)
                .assignments(ungraded)
                .gradedAssignments(graded)
                .build();
    }

    /* PLM-004 사이드바 배지: 전 학기 미채점 합 (overview 학기별 N+1 합산 대체) */
    @Override
    @Transactional(readOnly = true)
    public LmsGradingDto.UngradedCount getTotalUngradedCount(Long memberId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        int totalUngraded = gradingMapper.sumUngradedAcrossSemesters(professorLmsPrfId);
        return LmsGradingDto.UngradedCount.builder()
                .totalUngraded(totalUngraded)
                .build();
    }

    /* PLM-004 과제 채점 상세 */
    @Override
    @Transactional(readOnly = true)
    public LmsGradingDto.Detail getAssignmentDetail(Long memberId, Long assignmentId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        LmsGradingDto.Detail detail = gradingMapper.selectAssignmentHeader(assignmentId, professorLmsPrfId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 담당하는 과제가 아닙니다.");
        }

        List<LmsGradingDto.Submission> submissions =
                gradingMapper.selectAssignmentSubmissions(assignmentId, professorLmsPrfId);
        Map<Long, LmsGradingDto.SubmissionFile> fileBySubmission =
                latestFilesBySubmission(gradingMapper.selectAssignmentFiles(assignmentId));

        int gradedCount = 0;
        int ungradedCount = 0;
        for (LmsGradingDto.Submission submission : submissions) {
            enrichSubmission(submission, fileBySubmission.get(submission.getSubmissionId()));
            if (submission.isGraded()) {
                gradedCount++;
            } else if (isSubmitted(submission)) {
                ungradedCount++; // 제출했으나 미채점
            }
        }

        detail.setMaxScore(DEFAULT_MAX_SCORE);
        detail.setGradedCount(gradedCount);
        detail.setUngradedCount(ungradedCount);
        detail.setSubmissions(submissions);
        return detail;
    }

    /* PLM-004 점수·피드백 저장 */
    @Override
    @Transactional
    public LmsGradingDto.Submission saveEvaluation(Long memberId, Long assignmentId, Long submissionId,
                                                   LmsGradingDto.SaveRequest request) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        if (gradingMapper.countOwnedSubmission(submissionId, assignmentId, professorLmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 강의의 제출이 아닙니다.");
        }

        BigDecimal score = request != null ? request.getScore() : null;
        if (score != null && (score.signum() < 0 || score.compareTo(BigDecimal.valueOf(DEFAULT_MAX_SCORE)) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "점수는 0~" + DEFAULT_MAX_SCORE + " 범위여야 합니다.");
        }
        String feedback = request != null ? request.getFeedback() : null;

        gradingMapper.upsertEvaluation(submissionId, score, feedback);
        log.info("채점 저장 memberId={} assignmentId={} submissionId={} scored={}",
                memberId, assignmentId, submissionId, score != null);

        LmsGradingDto.Submission updated = gradingMapper.selectSubmission(submissionId, professorLmsPrfId);
        if (updated != null) {
            enrichSubmission(updated, latestFile(gradingMapper.selectSubmissionFiles(submissionId)));
        }
        return updated;
    }

    /* PLM-004-01 제출 파일 다운로드 (인증 필요) */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadSubmissionFile(Long memberId, Long submissionId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        if (gradingMapper.countOwnedSubmissionById(submissionId, professorLmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 강의의 제출이 아닙니다.");
        }

        List<LmsGradingDto.FileRow> files = gradingMapper.selectSubmissionFiles(submissionId);
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "제출 파일이 없습니다.");
        }
        LmsGradingDto.FileRow file = files.get(0); // 최신 첨부
        String directoryPath = resolveDirectory(file.getOrgUrl());
        return storageService.downloadFile(directoryPath, file.getTrnFileName(), file.getFileName());
    }

    /* ===== helpers ===== */

    private Long requireProfessorLmsPrfId(Long memberId) {
        Long professorLmsPrfId = gradingMapper.findLmsPrfIdByMemberId(memberId);
        if (professorLmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }
        return professorLmsPrfId;
    }

    /* 제출했고(미제출 아님) 아직 채점 안 된 경우 */
    private boolean isSubmitted(LmsGradingDto.Submission submission) {
        return submission.getSubmissionId() != null
                && !SUBMITTED_NONE.equals(submission.getSubmissionStatus());
    }

    /* 제출별 최신 첨부 1건 맵 (selectAssignmentFiles가 제출별·등록일 DESC 정렬이라 첫 행이 최신) */
    private Map<Long, LmsGradingDto.SubmissionFile> latestFilesBySubmission(List<LmsGradingDto.FileRow> rows) {
        Map<Long, LmsGradingDto.SubmissionFile> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (LmsGradingDto.FileRow row : rows) {
            map.computeIfAbsent(row.getSubmissionId(), key -> toFile(row));
        }
        return map;
    }

    private LmsGradingDto.SubmissionFile latestFile(List<LmsGradingDto.FileRow> rows) {
        return (rows == null || rows.isEmpty()) ? null : toFile(rows.get(0));
    }

    private LmsGradingDto.SubmissionFile toFile(LmsGradingDto.FileRow row) {
        return LmsGradingDto.SubmissionFile.builder()
                .fileName(row.getFileName())
                .fileSize(row.getFileSize())
                .fileUrl(String.format(FILE_DOWNLOAD_PATH, row.getSubmissionId()))
                .contentType(row.getContentType())
                .build();
    }

    private void enrichSubmission(LmsGradingDto.Submission submission, LmsGradingDto.SubmissionFile file) {
        submission.setGraded(submission.getScore() != null);
        if (submission.getFeedback() == null) {
            submission.setFeedback(""); // FE는 feedback 비null 문자열 기대
        }
        submission.setFile(file);
    }

    /* ORG_URL("/uploads/.../trn.ext") → 물리 디렉토리(uploadRoot + 하위경로). 프로필 이미지와 동일 저장 관례 가정 */
    private String resolveDirectory(String orgUrl) {
        if (orgUrl == null || orgUrl.isBlank()) {
            return uploadRoot;
        }
        String relative = orgUrl.startsWith(UPLOAD_WEB_PREFIX) ? orgUrl.substring(UPLOAD_WEB_PREFIX.length()) : orgUrl;
        int lastSlash = relative.lastIndexOf('/');
        String dirRelative = lastSlash > 0 ? relative.substring(0, lastSlash) : "";
        return uploadRoot + dirRelative.replace("/", File.separator);
    }
}
