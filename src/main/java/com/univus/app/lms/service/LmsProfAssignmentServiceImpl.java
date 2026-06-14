package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.common.StorageService;
import com.univus.app.lms.dto.LmsProfAssignmentDto;
import com.univus.app.lms.mapper.LmsProfAssignmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * PLM-006 교수 과제 관리 ServiceImpl.
 * - 구조는 PLM-005(강의 자료 업로드)와 동형: 첨부 다중(등록=files / 수정=files 추가 + removeAttachmentIds 제거),
 *   소유권 검증, StorageService(로컬 디스크) 재사용. 차이 = 마감일(dueDate) + 제출/채점/수강생 집계.
 * - 검증 실패는 ResponseStatusException(400/403)으로 던짐(전역 핸들러가 상태코드 그대로 JSON 매핑 — 500 회피).
 * - 집계: 매퍼가 submitted/graded/total을 채우고, ungraded = max(0, submitted - graded)를 여기서 계산(PLM-004 동일 산식).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsProfAssignmentServiceImpl implements LmsProfAssignmentService {

    private final LmsProfAssignmentMapper lmsProfAssignmentMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    // 저장 하위 폴더 / 웹 접근 경로 (PLM-005 material과 동일 관례 — 인증 보호, permitAll 아님)
    private static final String ASSIGNMENT_SUBDIR = "lms" + File.separator + "professor" + File.separator + "assignment";
    private static final String ASSIGNMENT_URL_PREFIX = "/uploads/lms/professor/assignment/";

    // 업로드 제약 — FE(lmsProfessorAssignmentsApi.ts ASSIGNMENT_ALLOWED_EXTS)와 동일 목록(PLM-005와 동일).
    // ⚠️ 목록 변경 시 FE·BE 동시 수정 필요(자동 동기화 없음).
    private static final Set<String> ALLOWED_EXTS = Set.of(
            "mp4", "avi", "mov", "wmv",
            "mp3", "m4a", "wav",
            "pdf", "hwp", "hwpx", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt",
            "jpg", "jpeg", "png", "gif",
            "zip");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024;  // 단일 파일 5GB
    private static final long MAX_TOTAL_SIZE = 5L * 1024 * 1024 * 1024; // 요청 합계 5GB
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_TITLE_LENGTH = 200; // LEC_ASN_TITLE VARCHAR2(200)

    // datetime-local 포맷 "YYYY-MM-DDTHH:mm" — TO_TIMESTAMP 전 형식 가드(틀리면 ORA→500 대신 400)
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}");

    @Override
    @Transactional(readOnly = true)
    public List<LmsProfAssignmentDto.LectureResDto> getLectures(Long memberId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        return lmsProfAssignmentMapper.selectLecturesByProfessor(lmsPrfId).stream()
                .map(this::toLectureResDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginateUtilRestApiRes<LmsProfAssignmentDto.AssignmentResDto> getCourseAssignments(
            Long memberId, Long lecId, int page, int size) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        long total = lmsProfAssignmentMapper.countAssignmentsByCourse(lmsPrfId, lecId);
        List<LmsProfAssignmentDto.AssignmentResDto> assignments = lmsProfAssignmentMapper.selectAssignmentsByCoursePaged(
                        lmsPrfId, lecId, PaginateUtilRestApi.offset(safePage, safeSize), safeSize).stream()
                .map(this::toAssignmentResDto)
                .collect(Collectors.toList());
        attachAssignmentAttachments(assignments);
        return PaginateUtilRestApi.of(assignments, total, safePage, safeSize);
    }

    /* 현재 페이지 과제들의 유효(ACT) 첨부를 조회해 각 AssignmentResDto에 그룹핑 (빈 목록이면 IN() 방지) */
    private void attachAssignmentAttachments(List<LmsProfAssignmentDto.AssignmentResDto> assignments) {
        if (assignments.isEmpty()) {
            return;
        }
        List<Long> assignmentIds = assignments.stream()
                .map(LmsProfAssignmentDto.AssignmentResDto::getAssignmentId)
                .collect(Collectors.toList());
        Map<Long, List<LmsProfAssignmentDto.AttachmentResDto>> grouped =
                lmsProfAssignmentMapper.selectActiveAttachmentsByAssignmentIds(assignmentIds).stream()
                        .collect(Collectors.groupingBy(
                                LmsProfAssignmentDto.AttachmentRow::getAssignmentId,
                                Collectors.mapping(this::toAttachmentResDto, Collectors.toList())));
        for (LmsProfAssignmentDto.AssignmentResDto assignment : assignments) {
            assignment.setAttachments(grouped.getOrDefault(assignment.getAssignmentId(), Collections.emptyList()));
        }
    }

    @Override
    @Transactional
    public LmsProfAssignmentDto.AssignmentResDto createAssignment(Long memberId, LmsProfAssignmentDto.CreateReqDto request) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);

        String title = requireTitle(request.getTitle());
        String dueDate = requireDueDate(request.getDueDate());
        List<MultipartFile> files = normalizeFiles(request.getFiles());
        validateFiles(files);

        if (lmsProfAssignmentMapper.countOwnedLecture(request.getLecId(), lmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 담당하는 강의가 아닙니다.");
        }

        // 본체 INSERT (assignmentId는 selectKey로 채번, 상태=AVL 고정)
        LmsProfAssignmentDto.InsertParam param = LmsProfAssignmentDto.InsertParam.builder()
                .lecId(request.getLecId())
                .title(title)
                .dueDate(dueDate)
                .description(normalizeContent(request.getDescription()))
                .build();
        lmsProfAssignmentMapper.insertAssignment(param);

        for (MultipartFile file : files) {
            saveAttachment(param.getAssignmentId(), file);
        }

        log.info("과제 등록 lmsPrfId={} assignmentId={} lecId={} files={}",
                lmsPrfId, param.getAssignmentId(), request.getLecId(), files.size());
        return findAssignment(param.getAssignmentId());
    }

    @Override
    @Transactional
    public LmsProfAssignmentDto.AssignmentResDto updateAssignment(Long memberId, Long assignmentId,
                                                        LmsProfAssignmentDto.UpdateReqDto request) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        requireOwnedAssignment(assignmentId, lmsPrfId);

        String title = requireTitle(request.getTitle());
        String dueDate = requireDueDate(request.getDueDate());
        lmsProfAssignmentMapper.updateAssignment(
                assignmentId, title, dueDate, normalizeContent(request.getDescription()));

        // 기존 첨부 개별 제거 (소프트 무효화 — assignmentId 조건으로 본 과제의 첨부만)
        List<Long> removeIds = request.getRemoveAttachmentIds() == null
                ? Collections.emptyList()
                : request.getRemoveAttachmentIds();
        for (Long attachmentId : removeIds) {
            lmsProfAssignmentMapper.invalidateAttachmentById(assignmentId, attachmentId);
        }

        // 새 파일 추가 (교체 아님 — 기존 유효 첨부에 더해짐)
        List<MultipartFile> files = normalizeFiles(request.getFiles());
        validateFiles(files);
        for (MultipartFile file : files) {
            saveAttachment(assignmentId, file);
        }

        log.info("과제 수정 lmsPrfId={} assignmentId={} addedFiles={} removedAttachments={}",
                lmsPrfId, assignmentId, files.size(), removeIds.size());
        return findAssignment(assignmentId);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long memberId, Long assignmentId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        requireOwnedAssignment(assignmentId, lmsPrfId);

        // 디스크 정리 대상 확보 후 DB 삭제 (첨부 → 본체, FK 순서)
        List<LmsProfAssignmentDto.AttachmentDiskRow> attachments =
                lmsProfAssignmentMapper.selectAttachmentsByAssignmentId(assignmentId);
        lmsProfAssignmentMapper.deleteAttachments(assignmentId);
        lmsProfAssignmentMapper.deleteAssignment(assignmentId);

        // 디스크 파일 정리 — 실패해도 DB 삭제 유지(고아 파일 허용, 로그만)
        String directoryPath = uploadRoot + File.separator + ASSIGNMENT_SUBDIR;
        for (LmsProfAssignmentDto.AttachmentDiskRow attachment : attachments) {
            boolean deleted = storageService.deleteFile(directoryPath, attachment.getTrnFileName());
            if (!deleted) {
                log.warn("과제 첨부 디스크 파일 삭제 실패(고아 파일) assignmentId={} trnFileName={}",
                        assignmentId, attachment.getTrnFileName());
            }
        }
        log.info("과제 삭제 lmsPrfId={} assignmentId={} attachments={}", lmsPrfId, assignmentId, attachments.size());
    }

    /* 과제 단건 + 집계 + 유효 첨부 조립 */
    private LmsProfAssignmentDto.AssignmentResDto findAssignment(Long assignmentId) {
        LmsProfAssignmentDto.AssignmentRow row = lmsProfAssignmentMapper.selectAssignmentById(assignmentId);
        if (row == null) {
            return null;
        }
        LmsProfAssignmentDto.AssignmentResDto assignment = toAssignmentResDto(row);
        assignment.setAttachments(
                lmsProfAssignmentMapper.selectActiveAttachmentsByAssignmentId(assignmentId).stream()
                        .map(this::toAttachmentResDto)
                        .collect(Collectors.toList()));
        return assignment;
    }

    /* ===== 매핑 row → 응답 ResDto 변환 ===== */

    private LmsProfAssignmentDto.LectureResDto toLectureResDto(LmsProfAssignmentDto.LectureRow row) {
        return LmsProfAssignmentDto.LectureResDto.builder()
                .lecId(row.getLecId())
                .courseName(row.getCourseName())
                .lecSection(row.getLecSection())
                .year(row.getYear())
                .termCode(row.getTermCode())
                .lecValStatus(row.getLecValStatus())
                .build();
    }

    /* 미채점 = max(0, 제출 - 채점완료) (PLM-004 채점 현황과 동일 산식) — 변환 지점에서 계산 */
    private LmsProfAssignmentDto.AssignmentResDto toAssignmentResDto(LmsProfAssignmentDto.AssignmentRow row) {
        int ungraded = Math.max(0, row.getSubmittedCount() - row.getGradedCount());
        return LmsProfAssignmentDto.AssignmentResDto.builder()
                .assignmentId(row.getAssignmentId())
                .lecId(row.getLecId())
                .courseName(row.getCourseName())
                .lecSection(row.getLecSection())
                .year(row.getYear())
                .termCode(row.getTermCode())
                .title(row.getTitle())
                .description(row.getDescription())
                .dueDate(row.getDueDate())
                .valStatus(row.getValStatus())
                .submittedCount(row.getSubmittedCount())
                .gradedCount(row.getGradedCount())
                .ungradedCount(ungraded)
                .totalStudents(row.getTotalStudents())
                .build();
    }

    private LmsProfAssignmentDto.AttachmentResDto toAttachmentResDto(LmsProfAssignmentDto.AttachmentRow row) {
        return LmsProfAssignmentDto.AttachmentResDto.builder()
                .attachmentId(row.getAttachmentId())
                .fileName(row.getFileName())
                .fileExt(row.getFileExt())
                .fileSize(row.getFileSize())
                .build();
    }

    /* 파일 저장(디스크) + 첨부 메타 INSERT */
    private void saveAttachment(Long assignmentId, MultipartFile file) {
        String directoryPath = uploadRoot + File.separator + ASSIGNMENT_SUBDIR;
        String trnFileName = storageService.uploadFileToServer(file, directoryPath);
        String orgFileName = file.getOriginalFilename();
        String orgUrl = ASSIGNMENT_URL_PREFIX + trnFileName;
        String extType = extractExtension(orgFileName);
        lmsProfAssignmentMapper.insertAttachment(
                assignmentId, orgFileName, trnFileName, orgUrl, file.getSize(), extType);
    }

    /* 교수 LMS 프로필 확인 — 없으면 403 (PLM-003/004/005와 동일) */
    private Long requireProfessorLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsProfAssignmentMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    /* 과제 소유권 확인 — 본인 강의 과제 아니면 403 */
    private void requireOwnedAssignment(Long assignmentId, Long lmsPrfId) {
        if (lmsProfAssignmentMapper.countOwnedAssignment(assignmentId, lmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 등록한 과제가 아닙니다.");
        }
    }

    /* 제목 검증 — trim 후 필수·200자 (DTO @Valid와 별개로 400 보장) */
    private String requireTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과제명을 입력해 주세요.");
        }
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "과제명은 " + MAX_TITLE_LENGTH + "자 이내여야 합니다.");
        }
        return trimmed;
    }

    /* 마감일 검증 — "YYYY-MM-DDTHH:mm" 형식(틀리면 TO_TIMESTAMP ORA→500 대신 400) */
    private String requireDueDate(String dueDate) {
        String trimmed = dueDate == null ? "" : dueDate.trim();
        if (!DUE_DATE_PATTERN.matcher(trimmed).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "마감 일시 형식이 올바르지 않습니다.");
        }
        return trimmed;
    }

    /* 설명 정규화 — 빈 문자열/공백뿐이면 null 저장 (CLOB NULL 허용. sanitize는 표시단 FE 책임) */
    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content;
    }

    /* null/빈 파트를 걸러낸 실제 업로드 파일 목록 */
    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) {
            return Collections.emptyList();
        }
        List<MultipartFile> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                result.add(file);
            }
        }
        return result;
    }

    /* 파일 검증 — FE와 동일: 단일(확장자 화이트리스트/파일명 255자/5GB) + 요청 합계 5GB */
    private void validateFiles(List<MultipartFile> files) {
        long total = 0;
        for (MultipartFile file : files) {
            validateFile(file);
            total += file.getSize();
        }
        if (total > MAX_TOTAL_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드 파일 전체 합계는 5GB를 초과할 수 없습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명이 올바르지 않습니다.");
        }
        if (name.length() > MAX_FILENAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명은 " + MAX_FILENAME_LENGTH + "자 이내여야 합니다.");
        }
        String ext = extractExtension(name);
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "허용되지 않는 파일 형식입니다. 영상(MP4·AVI·MOV·WMV)·음성(MP3·M4A·WAV)·문서(PDF·HWP·DOC·PPT·XLS·TXT)·이미지(JPG·PNG·GIF)·ZIP만 업로드할 수 있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단일 파일 용량은 5GB를 초과할 수 없습니다.");
        }
    }

    /* 파일명 → 소문자 확장자 */
    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1).toLowerCase();
    }
}
