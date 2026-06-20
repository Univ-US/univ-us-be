package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.common.StorageService;
import com.univus.app.lms.dto.LmsProfGradingDto;
import com.univus.app.lms.mapper.LmsProfGradingMapper;
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
public class LmsProfGradingServiceImpl implements LmsProfGradingService {

    private final LmsProfGradingMapper gradingMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    // TODO maxScore: LECTURE_ASSIGNMENT에 만점 컬럼이 없어 100 고정. 추후 재검토(컬럼 추가 vs 고정).
    private static final int DEFAULT_MAX_SCORE = 100;

    private static final String UPLOAD_WEB_PREFIX = "/uploads"; // ORG_URL 웹 접두어
    private static final String FILE_DOWNLOAD_PATH = "/api/lms/professor/grading/submissions/%d/file";

    /* PLM-004 채점 개요 — 배너 카운트(미채점 합 + 과목별). 년도/학기 null이면 전체 범위 */
    @Override
    @Transactional(readOnly = true)
    public LmsProfGradingDto.OverviewResDto getOverview(Long memberId, Integer year, String termCode) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        int totalUngraded = gradingMapper.sumUngraded(professorLmsPrfId, year, termCode);
        List<LmsProfGradingDto.CourseCountRow> rows =
                gradingMapper.selectUngradedByCourse(professorLmsPrfId, year, termCode);
        List<LmsProfGradingDto.CourseCountResDto> byCourse = new ArrayList<>(rows.size());
        for (LmsProfGradingDto.CourseCountRow row : rows) {
            byCourse.add(LmsProfGradingDto.CourseCountResDto.builder()
                    .courseName(row.getCourseName())
                    .count(row.getCount())
                    .build());
        }
        return LmsProfGradingDto.OverviewResDto.builder()
                .totalUngraded(totalUngraded)
                .byCourse(byCourse)
                .build();
    }

    /* PLM-004 과제 목록 1페이지 — 서버 페이지네이션(미채점/채점 분리 + 년도/학기 필터) */
    @Override
    @Transactional(readOnly = true)
    public PaginateUtilRestApiRes<LmsProfGradingDto.AssignmentResDto> getAssignments(
            Long memberId, boolean graded, Integer year, String termCode, int page, int size) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        boolean ungradedOnly = !graded; // 미채점 목록=ungraded>0 / 채점 목록=<=0
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        long total = gradingMapper.countGradingAssignments(professorLmsPrfId, year, termCode, ungradedOnly);
        List<LmsProfGradingDto.AssignmentRow> rows = gradingMapper.selectGradingAssignmentsPaged(
                professorLmsPrfId, year, termCode, ungradedOnly,
                PaginateUtilRestApi.offset(safePage, safeSize), safeSize);
        // 매핑 row → 응답 (미채점 수 계산·만점 세팅)
        List<LmsProfGradingDto.AssignmentResDto> content = new ArrayList<>(rows.size());
        for (LmsProfGradingDto.AssignmentRow row : rows) {
            content.add(LmsProfGradingDto.AssignmentResDto.builder()
                    .assignmentId(row.getAssignmentId())
                    .courseName(row.getCourseName())
                    .lecSection(row.getLecSection())
                    .lecAsnTitle(row.getLecAsnTitle())
                    .lecAsnDueDate(row.getLecAsnDueDate())
                    .submittedCount(row.getSubmittedCount())
                    .gradedCount(row.getGradedCount())
                    .ungradedCount(Math.max(0, row.getTotalStudents() - row.getGradedCount())) // 미채점=총원−채점
                    .maxScore(DEFAULT_MAX_SCORE)
                    .build());
        }
        return PaginateUtilRestApi.of(content, total, safePage, safeSize);
    }

    /* PLM-004 사이드바 배지: 전 학기 미채점 합 (year/termCode null = 전체) */
    @Override
    @Transactional(readOnly = true)
    public LmsProfGradingDto.UngradedCountResDto getTotalUngradedCount(Long memberId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        int totalUngraded = gradingMapper.sumUngraded(professorLmsPrfId, null, null);
        return LmsProfGradingDto.UngradedCountResDto.builder()
                .totalUngraded(totalUngraded)
                .build();
    }

    /* PLM-004 과제 채점 상세 */
    @Override
    @Transactional(readOnly = true)
    public LmsProfGradingDto.DetailResDto getAssignmentDetail(Long memberId, Long assignmentId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        LmsProfGradingDto.DetailRow header = gradingMapper.selectAssignmentHeader(assignmentId, professorLmsPrfId);
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 담당하는 과제가 아닙니다.");
        }

        List<LmsProfGradingDto.SubmissionRow> rows =
                gradingMapper.selectAssignmentSubmissions(assignmentId, professorLmsPrfId);
        Map<Long, LmsProfGradingDto.SubmissionFileResDto> fileBySubmission =
                latestFilesBySubmission(gradingMapper.selectAssignmentFiles(assignmentId));

        int gradedCount = 0;
        List<LmsProfGradingDto.SubmissionResDto> submissions = new ArrayList<>(rows.size());
        for (LmsProfGradingDto.SubmissionRow row : rows) {
            LmsProfGradingDto.SubmissionResDto submission =
                    toSubmissionResDto(row, fileBySubmission.get(row.getSubmissionId()));
            submissions.add(submission);
            if (submission.isGraded()) {
                gradedCount++;
            }
        }
        // 미채점 = 채점 안 된 수강생 수(총원 − 채점완료). rows = 수강생 전체(미제출 포함, selectAssignmentSubmissions) → rows.size() = 총원
        int ungradedCount = Math.max(0, rows.size() - gradedCount);

        return LmsProfGradingDto.DetailResDto.builder()
                .assignmentId(header.getAssignmentId())
                .courseName(header.getCourseName())
                .lecAsnTitle(header.getLecAsnTitle())
                .lecAsnDueDate(header.getLecAsnDueDate())
                .maxScore(DEFAULT_MAX_SCORE)
                .gradedCount(gradedCount)
                .ungradedCount(ungradedCount)
                .submissions(submissions)
                .build();
    }

    /* PLM-004 점수·피드백 저장 */
    @Override
    @Transactional
    public LmsProfGradingDto.SubmissionResDto saveEvaluation(Long memberId, Long assignmentId, Long submissionId,
                                                             LmsProfGradingDto.SaveReqDto request) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        if (gradingMapper.countOwnedSubmission(submissionId, assignmentId, professorLmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 강의의 제출이 아닙니다.");
        }

        // 점수 검증(0~100·소수 2자리)은 SaveReqDto의 Bean Validation(@Valid)에서 수행 →
        // 위반 시 LmsRestExceptionHandler가 400으로 응답. null = 미채점 되돌림(검증 통과).
        BigDecimal score = request != null ? request.getAsnSbmEvlScore() : null;
        String feedback = request != null ? request.getAsnSbmEvlFeedback() : null;

        gradingMapper.upsertEvaluation(submissionId, score, feedback);
        log.info("채점 저장 memberId={} assignmentId={} submissionId={} scored={}",
                memberId, assignmentId, submissionId, score != null);

        LmsProfGradingDto.SubmissionRow updated = gradingMapper.selectSubmission(submissionId, professorLmsPrfId);
        if (updated == null) {
            return null;
        }
        return toSubmissionResDto(updated, latestFile(gradingMapper.selectSubmissionFiles(submissionId)));
    }

    /* PLM-004-01 제출 파일 다운로드 (인증 필요) */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadSubmissionFile(Long memberId, Long submissionId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(memberId);
        if (gradingMapper.countOwnedSubmissionById(submissionId, professorLmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 강의의 제출이 아닙니다.");
        }

        List<LmsProfGradingDto.FileRow> files = gradingMapper.selectSubmissionFiles(submissionId);
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "제출 파일이 없습니다.");
        }
        LmsProfGradingDto.FileRow file = files.get(0); // 최신 첨부
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

    /* 제출별 최신 첨부 1건 맵 (selectAssignmentFiles가 제출별·등록일 DESC 정렬이라 첫 행이 최신) */
    private Map<Long, LmsProfGradingDto.SubmissionFileResDto> latestFilesBySubmission(List<LmsProfGradingDto.FileRow> rows) {
        Map<Long, LmsProfGradingDto.SubmissionFileResDto> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (LmsProfGradingDto.FileRow row : rows) {
            map.computeIfAbsent(row.getSubmissionId(), key -> toFile(row));
        }
        return map;
    }

    private LmsProfGradingDto.SubmissionFileResDto latestFile(List<LmsProfGradingDto.FileRow> rows) {
        return (rows == null || rows.isEmpty()) ? null : toFile(rows.get(0));
    }

    private LmsProfGradingDto.SubmissionFileResDto toFile(LmsProfGradingDto.FileRow row) {
        return LmsProfGradingDto.SubmissionFileResDto.builder()
                .fileName(row.getFileName())
                .fileSize(row.getFileSize())
                .fileUrl(String.format(FILE_DOWNLOAD_PATH, row.getSubmissionId()))
                .contentType(row.getContentType())
                .build();
    }

    /* 매핑 row → 응답 SubmissionResDto (채점 여부·피드백 null→""·파일 보강) */
    private LmsProfGradingDto.SubmissionResDto toSubmissionResDto(LmsProfGradingDto.SubmissionRow row,
                                                                 LmsProfGradingDto.SubmissionFileResDto file) {
        return LmsProfGradingDto.SubmissionResDto.builder()
                .submissionId(row.getSubmissionId())
                .memberId(row.getMemberId())
                .studentName(row.getStudentName())
                .studentNo(row.getStudentNo())
                .lecAsnSbmRegDate(row.getLecAsnSbmRegDate())
                .lecAsnSbmStatus(row.getLecAsnSbmStatus())
                .asnSbmEvlScore(row.getAsnSbmEvlScore())
                .asnSbmEvlFeedback(row.getAsnSbmEvlFeedback() == null ? "" : row.getAsnSbmEvlFeedback()) // FE는 비null 문자열 기대
                .graded(row.getAsnSbmEvlScore() != null)
                .file(file)
                .build();
    }

    /* ORG_URL("/uploads/.../trn.ext") → 물리 디렉토리(uploadRoot + 하위경로). */
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
