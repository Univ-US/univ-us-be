package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.lms.code.LecAsnSbmStatusCode;
import com.univus.app.lms.dto.LmsStuAssignmentsDto;
import com.univus.app.lms.mapper.LmsStuAssignmentsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LmsStuAssignmentsServiceImpl implements LmsStuAssignmentsService {

    private static final Map<String, String> TERM_LABELS = Map.of(
            "SM1", "1학기",
            "SMR", "여름 계절",
            "SM2", "2학기",
            "WNT", "겨울 계절");

    private static final Set<String> SUBMITTABLE_VAL_STATUS = Set.of("AVL", "MOD", "LAT");
    private static final String NOT_SUBMITTED = LecAsnSbmStatusCode.NOT_SUBMITTED.getCode();
    private static final String SUBMITTED = LecAsnSbmStatusCode.SUBMITTED.getCode();
    private static final String GRADED = LecAsnSbmStatusCode.GRADED.getCode();
    private static final int DEFAULT_MAX_SCORE = 100;
    private static final int MAX_MEMO_LENGTH = 1000;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTS = Set.of(
            "mp4", "avi", "mov", "wmv",
            "mp3", "m4a", "wav",
            "pdf", "hwp", "hwpx", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt",
            "jpg", "jpeg", "png", "gif",
            "zip");
    private static final Pattern SAFE_FILENAME = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final DateTimeFormatter DUE_PARSE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String SUBMISSION_SUBDIR = "lms" + File.separator + "student" + File.separator + "assignment";
    private static final String SUBMISSION_URL_PREFIX = "/uploads/lms/student/assignment/";
    private static final String SUBMISSION_DOWNLOAD_PATH = "/api/lms/student/assignments/submissions/%d/file";
    private static final String UPLOAD_WEB_PREFIX = "/uploads";

    private final LmsStuAssignmentsMapper lmsStuAssignmentsMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    @Override
    @Transactional(readOnly = true)
    public LmsStuAssignmentsDto.AssignmentsResultResDto getAssignments(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        List<LmsStuAssignmentsDto.AssignmentFlatRow> rows =
                lmsStuAssignmentsMapper.selectAssignmentRows(lmsPrfId);

        Map<String, SemesterAccumulator> semesters = new LinkedHashMap<>();
        for (LmsStuAssignmentsDto.AssignmentFlatRow row : rows) {
            String key = row.getYear() + ":" + row.getTermCode();
            SemesterAccumulator semester = semesters.computeIfAbsent(
                    key,
                    ignored -> new SemesterAccumulator(row.getYear(), row.getTermCode()));
            semester.add(row);
        }

        return LmsStuAssignmentsDto.AssignmentsResultResDto.builder()
                .semesters(semesters.values().stream()
                        .map(SemesterAccumulator::toResponse)
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuAssignmentsDto.SubmittableAssignmentResDto> getSubmittableAssignments(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        return lmsStuAssignmentsMapper.selectSubmittableRows(lmsPrfId).stream()
                .map(this::toSubmittableResponse)
                .toList();
    }

    @Override
    @Transactional
    public void submitAssignment(Long memberId, Long assignmentId, LmsStuAssignmentsDto.SubmitReqDto request) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        LmsStuAssignmentsDto.SubmitAccessRow access =
                lmsStuAssignmentsMapper.selectSubmitAccess(assignmentId, lmsPrfId);
        if (access == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 수강 중인 강의의 과제가 아닙니다.");
        }
        validateSubmittable(access);

        MultipartFile file = request == null ? null : request.getFile();
        validateFile(file);
        String memo = normalizeMemo(request == null ? null : request.getMemo());

        Long submissionId = access.getSubmissionId();
        if (submissionId == null) {
            LmsStuAssignmentsDto.SubmitParam param = LmsStuAssignmentsDto.SubmitParam.builder()
                    .assignmentId(assignmentId)
                    .lmsPrfId(lmsPrfId)
                    .memo(memo)
                    .build();
            lmsStuAssignmentsMapper.insertSubmission(param);
            submissionId = param.getSubmissionId();
        } else {
            int updated = lmsStuAssignmentsMapper.updateSubmissionAsSubmitted(submissionId, memo);
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 제출되었거나 상태가 변경된 과제입니다.");
            }
        }

        saveAttachment(submissionId, file);
        log.info("학생 과제 제출 memberId={} lmsPrfId={} assignmentId={} submissionId={}",
                memberId, lmsPrfId, assignmentId, submissionId);
    }

    @Override
    @Transactional
    public void updateSubmission(Long memberId, Long submissionId,
                                 LmsStuAssignmentsDto.UpdateSubmissionReqDto request) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        LmsStuAssignmentsDto.SubmitAccessRow access =
                lmsStuAssignmentsMapper.selectSubmissionAccess(submissionId, lmsPrfId);
        if (access == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 제출 내역이 아닙니다.");
        }
        validateEditable(access);

        MultipartFile file = request == null ? null : request.getFile();
        boolean hasNewFile = file != null && !file.isEmpty();
        if (hasNewFile) {
            validateFile(file);
        }

        boolean removeExistingFile = request != null && Boolean.TRUE.equals(request.getRemoveExistingFile());
        if (removeExistingFile && !hasNewFile) {
            lmsStuAssignmentsMapper.invalidateSubmissionAttachments(submissionId);
            int updated = lmsStuAssignmentsMapper.updateSubmissionAsNotSubmitted(submissionId);
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "수정할 수 없는 제출 상태입니다.");
            }

            log.info("학생 과제 제출 미제출 전환 memberId={} lmsPrfId={} submissionId={}",
                    memberId, lmsPrfId, submissionId);
            return;
        }

        String memo = normalizeMemo(request == null ? null : request.getMemo());

        int updated = lmsStuAssignmentsMapper.updateSubmissionMemo(submissionId, memo);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "수정할 수 없는 제출 상태입니다.");
        }

        if (removeExistingFile || hasNewFile) {
            lmsStuAssignmentsMapper.invalidateSubmissionAttachments(submissionId);
        }
        if (hasNewFile) {
            saveAttachment(submissionId, file);
        }

        log.info("학생 과제 제출 수정 memberId={} lmsPrfId={} submissionId={} newFile={} removeExisting={}",
                memberId, lmsPrfId, submissionId, hasNewFile, removeExistingFile);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadSubmissionFile(Long memberId, Long submissionId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        LmsStuAssignmentsDto.SubmitAccessRow access =
                lmsStuAssignmentsMapper.selectSubmissionAccess(submissionId, lmsPrfId);
        if (access == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 제출 내역이 아닙니다.");
        }

        List<LmsStuAssignmentsDto.FileRow> files = lmsStuAssignmentsMapper.selectSubmissionFiles(submissionId);
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "제출 파일이 없습니다.");
        }
        LmsStuAssignmentsDto.FileRow file = files.get(0);
        String directoryPath = resolveDirectory(file.getOrgUrl());
        return storageService.downloadFile(directoryPath, file.getTrnFileName(), file.getFileName());
    }

    private Long requireStudentLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsStuAssignmentsMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    private void validateSubmittable(LmsStuAssignmentsDto.SubmitAccessRow access) {
        if (!SUBMITTABLE_VAL_STATUS.contains(access.getValStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제출할 수 없는 상태의 과제입니다.");
        }
        if (access.getOverdueFlag() != null && access.getOverdueFlag() == 1 && !"LAT".equals(access.getValStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "마감된 과제는 제출할 수 없습니다.");
        }
        String status = access.getSubmissionStatus();
        if (status != null && !NOT_SUBMITTED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 제출된 과제입니다.");
        }
    }

    private void validateEditable(LmsStuAssignmentsDto.SubmitAccessRow access) {
        if (!SUBMITTABLE_VAL_STATUS.contains(access.getValStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "수정할 수 없는 상태의 과제입니다.");
        }
        if (access.getOverdueFlag() != null && access.getOverdueFlag() == 1 && !"LAT".equals(access.getValStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "마감된 과제는 수정할 수 없습니다.");
        }
        if (!SUBMITTED.equals(access.getSubmissionStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "제출 완료 상태에서만 수정할 수 있습니다.");
        }
    }

    private LmsStuAssignmentsDto.SubmittableAssignmentResDto toSubmittableResponse(
            LmsStuAssignmentsDto.SubmittableRow row) {
        boolean late = "LAT".equals(row.getValStatus());
        String status = late ? "EXTENDED" : "OPEN";
        return LmsStuAssignmentsDto.SubmittableAssignmentResDto.builder()
                .id(row.getAssignmentId())
                .title(row.getTitle())
                .courseName(row.getCourseName())
                .dueLabel(row.getDueLabel())
                .status(status)
                .dDay(toDDay(row.getDueIso()))
                .note(late ? "지각 제출 허용" : null)
                .badge(null)
                .dotColor(late ? "bg-orange-500" : "bg-emerald-500")
                .guide(LmsStuAssignmentsDto.SubmitGuideResDto.builder()
                        .courseName(row.getCourseName())
                        .professor(row.getProfessor())
                        .lines(buildGuideLines(row.getContent()))
                        .build())
                .build();
    }

    private List<String> buildGuideLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content != null && !content.isBlank()) {
            lines.add(content);
        } else {
            lines.add("과제 설명이 없습니다.");
        }
        lines.add("제출 형식: 허용 확장자 파일 1개 · 최대 5GB");
        return lines;
    }

    private String toDDay(String dueIso) {
        if (dueIso == null || dueIso.isBlank()) {
            return null;
        }
        LocalDate dueDate = LocalDateTime.parse(dueIso, DUE_PARSE_FORMAT).toLocalDate();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (days < 0) {
            return null;
        }
        String monthDay = dueDate.format(DateTimeFormatter.ofPattern("MM.dd"));
        return "D-" + days + " · " + monthDay;
    }

    private static String semesterLabel(Integer year, String termCode) {
        return year + "년 " + TERM_LABELS.getOrDefault(termCode, termCode);
    }

    private static boolean isOverdue(LmsStuAssignmentsDto.AssignmentFlatRow row) {
        return row.getOverdueFlag() != null && row.getOverdueFlag() == 1;
    }

    private static String resolveStatus(LmsStuAssignmentsDto.AssignmentFlatRow row) {
        if (row.getScore() != null || GRADED.equals(row.getSubmissionStatus())) {
            return GRADED;
        }
        if (row.getSubmissionId() != null && !NOT_SUBMITTED.equals(row.getSubmissionStatus())) {
            return SUBMITTED;
        }
        return NOT_SUBMITTED;
    }

    private LmsStuAssignmentsDto.StudentAssignmentResDto toAssignmentResponse(
            LmsStuAssignmentsDto.AssignmentFlatRow row) {
        String status = resolveStatus(row);
        LmsStuAssignmentsDto.SubmissionFileResDto file = row.getFileName() == null
                ? null
                : LmsStuAssignmentsDto.SubmissionFileResDto.builder()
                .fileName(row.getFileName())
                .fileSize(row.getFileSize())
                .fileUrl(row.getSubmissionId() == null ? null : String.format(SUBMISSION_DOWNLOAD_PATH, row.getSubmissionId()))
                .contentType(row.getFileExt())
                .build();
        LmsStuAssignmentsDto.AssignmentFeedbackResDto feedback = row.getScore() == null
                ? null
                : LmsStuAssignmentsDto.AssignmentFeedbackResDto.builder()
                .score(row.getScore())
                .maxScore(DEFAULT_MAX_SCORE)
                .courseName(row.getCourseName())
                .professor(row.getProfessor())
                .comment(row.getFeedback() == null ? "" : row.getFeedback())
                .build();

        return LmsStuAssignmentsDto.StudentAssignmentResDto.builder()
                .id(row.getAssignmentId())
                .submissionId(row.getSubmissionId())
                .courseName(row.getCourseName())
                .lecSection(row.getLecSection())
                .title(row.getTitle())
                .content(row.getContent())
                .dueDate(row.getDueDate())
                .status(status)
                .overdue(isOverdue(row))
                .score(row.getScore())
                .maxScore(DEFAULT_MAX_SCORE)
                .submittedAt(row.getSubmittedAt())
                .submissionMemo(row.getSubmissionMemo())
                .file(file)
                .feedback(feedback)
                .build();
    }

    private void saveAttachment(Long submissionId, MultipartFile file) {
        String directoryPath = uploadRoot + File.separator + SUBMISSION_SUBDIR;
        String trnFileName = storageService.uploadFileToServer(file, directoryPath);
        String orgFileName = file.getOriginalFilename();
        String orgUrl = SUBMISSION_URL_PREFIX + trnFileName;
        String extType = extractExtension(orgFileName);
        lmsStuAssignmentsMapper.insertSubmissionAttachment(
                submissionId, orgFileName, trnFileName, orgUrl, file.getSize(), extType);
    }

    private String resolveDirectory(String orgUrl) {
        if (orgUrl == null || orgUrl.isBlank()) {
            return uploadRoot;
        }
        String relative = orgUrl.startsWith(UPLOAD_WEB_PREFIX) ? orgUrl.substring(UPLOAD_WEB_PREFIX.length()) : orgUrl;
        int lastSlash = relative.lastIndexOf('/');
        String dirRelative = lastSlash > 0 ? relative.substring(0, lastSlash) : "";
        return uploadRoot + dirRelative.replace("/", File.separator);
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }
        String normalized = memo.trim();
        if (normalized.length() > MAX_MEMO_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제출 메모는 1000자 이내여야 합니다.");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제출 파일을 선택해 주세요.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명이 없는 파일은 제출할 수 없습니다.");
        }
        if (originalFilename.length() > MAX_FILENAME_LENGTH || SAFE_FILENAME.matcher(originalFilename).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명이 올바르지 않습니다.");
        }
        String ext = extractExtension(originalFilename);
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일은 최대 5GB까지 제출할 수 있습니다.");
        }
    }

    private String extractExtension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private class SemesterAccumulator {
        private final Integer year;
        private final String termCode;
        private final List<LmsStuAssignmentsDto.StudentAssignmentResDto> assignments = new ArrayList<>();

        private SemesterAccumulator(Integer year, String termCode) {
            this.year = year;
            this.termCode = termCode;
        }

        private void add(LmsStuAssignmentsDto.AssignmentFlatRow row) {
            assignments.add(toAssignmentResponse(row));
        }

        private LmsStuAssignmentsDto.SemesterAssignmentsResDto toResponse() {
            return LmsStuAssignmentsDto.SemesterAssignmentsResDto.builder()
                    .year(year)
                    .termCode(termCode)
                    .semesterLabel(semesterLabel(year, termCode))
                    .assignments(assignments)
                    .build();
        }
    }
}
