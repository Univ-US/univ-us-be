package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.StorageService;
import com.univus.app.lms.dto.LmsUploadDto;
import com.univus.app.lms.mapper.LmsProfessorUploadMapper;
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
import java.util.stream.Collectors;

/**
 * PLM-005 / PLM-005-01 교수 강의 자료 업로드 ServiceImpl.
 * - 첨부는 다중(2026-06-11 정책): 등록=여러 파일 / 수정=기존 유지 + 추가(files) + 개별 제거(removeAttachmentIds).
 *   "교체" 개념 없음 — 제거는 ATT_VAL_STATUS='DEL' 소프트 무효화(디스크 파일 보존).
 * - 검증 실패는 ResponseStatusException(400/403)으로 던짐
 *   (전역 핸들러가 상태코드 그대로 JSON 매핑 — 커스텀 예외/@Valid 단독 의존 시 500으로 새는 경로 회피)
 * - 파일 저장은 StorageService(로컬 디스크) 재사용. S3 전환은 추후 과제 — 전환 시 이 서비스의
 *   저장/삭제 호출부와 URL prefix만 교체하면 되도록 격리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsProfessorUploadServiceImpl implements LmsProfessorUploadService {

    private final LmsProfessorUploadMapper lmsProfessorUploadMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    // 저장 하위 폴더 / 웹 접근 경로 (프로필 이미지와 동일 관례 — 단 material은 permitAll 아님·인증 보호)
    private static final String MATERIAL_SUBDIR = "lms" + File.separator + "professor" + File.separator + "material";
    private static final String MATERIAL_URL_PREFIX = "/uploads/lms/professor/material/";

    // 업로드 제약 — FE(lmsProfessorUploadApi.ts UPLOAD_ALLOWED_EXTS)와 동일 목록.
    // 확장자는 공통코드 비관리(종류 많고 변화 잦음, 2026-06-11 정책) → 코드 상수 화이트리스트.
    // ⚠️ 목록 변경 시 FE·BE 동시 수정 필요(자동 동기화 없음).
    private static final Set<String> ALLOWED_EXTS = Set.of(
            "mp4", "avi", "mov", "wmv",                                            // 영상
            "mp3", "m4a", "wav",                                                   // 음성
            "pdf", "hwp", "hwpx", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", // 문서
            "jpg", "jpeg", "png", "gif",                                           // 이미지
            "zip");                                                                // 압축
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 단일 파일 5GB
    private static final long MAX_TOTAL_SIZE = 5L * 1024 * 1024 * 1024; // 요청 합계 5GB (multipart max-request-size와 동일)
    private static final int MAX_FILENAME_LENGTH = 255;               // LEC_UPL_ATT_ORG_FIL_NAME VARCHAR2(255 CHAR)

    @Override
    @Transactional(readOnly = true)
    public List<LmsUploadDto.Lecture> getLectures(Long memberId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        return lmsProfessorUploadMapper.selectLecturesByProfessor(lmsPrfId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginateUtilRestApiRes<LmsUploadDto.Material> getUploads(
            Long memberId, int page, int size, Integer year, String termCode) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        long total = lmsProfessorUploadMapper.countUploadsByProfessor(lmsPrfId, year, termCode);
        List<LmsUploadDto.Material> materials = lmsProfessorUploadMapper.selectUploadsPaged(
                lmsPrfId, year, termCode, PaginateUtilRestApi.offset(safePage, safeSize), safeSize);
        attachMaterialAttachments(materials);
        return PaginateUtilRestApi.of(materials, total, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public LmsUploadDto.UploadMeta getUploadsMeta(Long memberId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        long totalAll = lmsProfessorUploadMapper.countUploadsByProfessor(lmsPrfId, null, null);
        List<LmsUploadDto.SemesterOption> semesters =
                lmsProfessorUploadMapper.selectDistinctMaterialSemesters(lmsPrfId);
        return LmsUploadDto.UploadMeta.builder()
                .totalAll(totalAll)
                .semesters(semesters)
                .build();
    }

    /* 현재 페이지 자료들의 유효(ACT) 첨부를 조회해 각 Material에 그룹핑 (빈 목록이면 첨부 조회 생략 — IN() 방지) */
    private void attachMaterialAttachments(List<LmsUploadDto.Material> materials) {
        if (materials.isEmpty()) {
            return;
        }
        List<Long> uploadIds = materials.stream()
                .map(LmsUploadDto.Material::getUploadId)
                .collect(Collectors.toList());
        Map<Long, List<LmsUploadDto.Attachment>> grouped =
                lmsProfessorUploadMapper.selectActiveAttachmentsByUploadIds(uploadIds).stream()
                        .collect(Collectors.groupingBy(
                                LmsUploadDto.AttachmentListRow::getUploadId,
                                Collectors.mapping(this::toAttachment, Collectors.toList())));
        for (LmsUploadDto.Material material : materials) {
            material.setAttachments(grouped.getOrDefault(material.getUploadId(), Collections.emptyList()));
        }
    }

    @Override
    @Transactional
    public LmsUploadDto.Material createUpload(Long memberId, LmsUploadDto.CreateRequest request) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);

        String title = requireTitle(request.getTitle());
        // 파일은 선택·다중 — 텍스트(설명)만으로도 등록 가능
        List<MultipartFile> files = normalizeFiles(request.getFiles());
        validateFiles(files);

        if (lmsProfessorUploadMapper.countOwnedLecture(request.getLecId(), lmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 담당하는 강의가 아닙니다.");
        }

        // 본체 INSERT (uploadId는 selectKey로 채번)
        LmsUploadDto.InsertParam param = LmsUploadDto.InsertParam.builder()
                .lecId(request.getLecId())
                .title(title)
                .content(normalizeContent(request.getContent()))
                .build();
        lmsProfessorUploadMapper.insertUpload(param);

        for (MultipartFile file : files) {
            saveAttachment(param.getUploadId(), file);
        }

        log.info("강의자료 등록 lmsPrfId={} uploadId={} lecId={} files={}",
                lmsPrfId, param.getUploadId(), request.getLecId(), files.size());
        return findMaterial(param.getUploadId());
    }

    @Override
    @Transactional
    public LmsUploadDto.Material updateUpload(Long memberId, Long uploadId, LmsUploadDto.UpdateRequest request) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        requireOwnedUpload(uploadId, lmsPrfId);

        String title = requireTitle(request.getTitle());
        lmsProfessorUploadMapper.updateUpload(uploadId, title, normalizeContent(request.getContent()));

        // 기존 첨부 개별 제거 (소프트 무효화 — uploadId 조건으로 본 자료의 첨부만)
        List<Long> removeIds = request.getRemoveAttachmentIds() == null
                ? Collections.emptyList()
                : request.getRemoveAttachmentIds();
        for (Long attachmentId : removeIds) {
            lmsProfessorUploadMapper.invalidateAttachmentById(uploadId, attachmentId);
        }

        // 새 파일 추가 (교체 아님 — 기존 유효 첨부에 더해짐)
        List<MultipartFile> files = normalizeFiles(request.getFiles());
        validateFiles(files);
        for (MultipartFile file : files) {
            saveAttachment(uploadId, file);
        }

        log.info("강의자료 수정 lmsPrfId={} uploadId={} addedFiles={} removedAttachments={}",
                lmsPrfId, uploadId, files.size(), removeIds.size());
        return findMaterial(uploadId);
    }

    @Override
    @Transactional
    public void deleteUpload(Long memberId, Long uploadId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        requireOwnedUpload(uploadId, lmsPrfId);

        // 디스크 정리 대상 확보 후 DB 삭제 (첨부 → 본체, FK 순서)
        List<LmsUploadDto.AttachmentRow> attachments =
                lmsProfessorUploadMapper.selectAttachmentsByUploadId(uploadId);
        lmsProfessorUploadMapper.deleteAttachments(uploadId);
        lmsProfessorUploadMapper.deleteUpload(uploadId);

        // 디스크 파일 정리 — 실패해도 DB 삭제는 유지(고아 파일 허용, 로그만)
        String directoryPath = uploadRoot + File.separator + MATERIAL_SUBDIR;
        for (LmsUploadDto.AttachmentRow attachment : attachments) {
            boolean deleted = storageService.deleteFile(directoryPath, attachment.getTrnFileName());
            if (!deleted) {
                log.warn("강의자료 디스크 파일 삭제 실패(고아 파일) uploadId={} trnFileName={}",
                        uploadId, attachment.getTrnFileName());
            }
        }
        log.info("강의자료 삭제 lmsPrfId={} uploadId={} attachments={}", lmsPrfId, uploadId, attachments.size());
    }

    /* 자료 단건 + 유효 첨부 조립 */
    private LmsUploadDto.Material findMaterial(Long uploadId) {
        LmsUploadDto.Material material = lmsProfessorUploadMapper.selectUploadById(uploadId);
        if (material != null) {
            material.setAttachments(
                    lmsProfessorUploadMapper.selectActiveAttachmentsByUploadId(uploadId).stream()
                            .map(this::toAttachment)
                            .collect(Collectors.toList()));
        }
        return material;
    }

    private LmsUploadDto.Attachment toAttachment(LmsUploadDto.AttachmentListRow row) {
        return LmsUploadDto.Attachment.builder()
                .attachmentId(row.getAttachmentId())
                .fileName(row.getFileName())
                .fileExt(row.getFileExt())
                .fileSize(row.getFileSize())
                .build();
    }

    /* 파일 저장(디스크) + 첨부 메타 INSERT */
    private void saveAttachment(Long uploadId, MultipartFile file) {
        String directoryPath = uploadRoot + File.separator + MATERIAL_SUBDIR;
        String trnFileName = storageService.uploadFileToServer(file, directoryPath);
        String orgFileName = file.getOriginalFilename();
        String orgUrl = MATERIAL_URL_PREFIX + trnFileName;
        String extType = extractExtension(orgFileName);
        lmsProfessorUploadMapper.insertAttachment(
                uploadId, orgFileName, trnFileName, orgUrl, file.getSize(), extType);
    }

    /* 교수 LMS 프로필 확인 — 없으면 403 (PLM-003/004와 동일 패턴) */
    private Long requireProfessorLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsProfessorUploadMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    /* 자료 소유권 확인 — 본인 강의 자료 아니면 403 */
    private void requireOwnedUpload(Long uploadId, Long lmsPrfId) {
        if (lmsProfessorUploadMapper.countOwnedUpload(uploadId, lmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 등록한 자료가 아닙니다.");
        }
    }

    /* 제목 검증 — trim 후 필수·500자 (DTO @Valid와 별개로 400 보장) */
    private String requireTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해 주세요.");
        }
        if (trimmed.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목은 500자 이내여야 합니다.");
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

    /*
     * 파일 검증 — FE와 동일 기준: 단일 파일(확장자 화이트리스트/파일명 255자/5GB) + 요청 합계 5GB
     */
    private void validateFiles(List<MultipartFile> files) {
        long total = 0;
        for (MultipartFile file : files) {
            validateFile(file);
            total += file.getSize();
        }
        if (total > MAX_TOTAL_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "업로드 파일 전체 합계는 5GB를 초과할 수 없습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명이 올바르지 않습니다.");
        }
        if (name.length() > MAX_FILENAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "파일명은 " + MAX_FILENAME_LENGTH + "자 이내여야 합니다.");
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

    /* 파일명 → 소문자 확장자 ("lecture.MP4" → "mp4", 확장자 없으면 "") */
    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1).toLowerCase();
    }
}
