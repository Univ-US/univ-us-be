package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.common.StorageService;
import com.univus.app.lms.dto.LmsStuMaterialsDto;
import com.univus.app.lms.mapper.LmsStuMaterialsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SLM-006 학생 강의 자료 ServiceImpl.
 * 화면 = 수강 과목 1개 선택 → 그 과목 자료를 서버 페이지네이션(클라 slice 없음).
 * 소유권: 본인 수강(DRP 제외) 강의의 자료만 — 매퍼 ENROLLMENT INNER JOIN으로 IDOR 차단.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsStuMaterialsServiceImpl implements LmsStuMaterialsService {

    // 첨부 저장 하위 폴더 (교수 업로드와 동일 디렉토리 — 학생은 다운로드만)
    private static final String MATERIAL_SUBDIR = "lms" + File.separator + "professor" + File.separator + "material";

    private final LmsStuMaterialsMapper lmsStuMaterialsMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuMaterialsDto.LectureResDto> getLectures(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        return lmsStuMaterialsMapper.selectEnrolledLectures(lmsPrfId).stream()
                .map(this::toLectureResDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginateUtilRestApiRes<LmsStuMaterialsDto.MaterialResDto> getMaterials(
            Long memberId, Long lecId, int page, int size) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        long total = lmsStuMaterialsMapper.countMaterials(lmsPrfId, lecId);
        List<LmsStuMaterialsDto.MaterialResDto> materials = lmsStuMaterialsMapper.selectMaterialsPaged(
                        lmsPrfId, lecId, PaginateUtilRestApi.offset(safePage, safeSize), safeSize).stream()
                .map(this::toMaterialResDto)
                .collect(Collectors.toList());
        attachMaterialAttachments(materials);
        log.info("학생 강의자료 조회 lmsPrfId={} lecId={} page={} size={} total={}", lmsPrfId, lecId, safePage, safeSize, total);
        return PaginateUtilRestApi.of(materials, total, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        LmsStuMaterialsDto.AttachmentDownloadRow file =
                lmsStuMaterialsMapper.selectDownloadAttachment(attachmentId, lmsPrfId);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "강의 자료 첨부 파일을 찾을 수 없습니다.");
        }

        String directoryPath = uploadRoot + File.separator + MATERIAL_SUBDIR;
        log.info("학생 강의자료 첨부 다운로드 lmsPrfId={} attachmentId={}", lmsPrfId, attachmentId);
        return storageService.downloadFile(directoryPath, file.getTrnFileName(), file.getFileName());
    }

    /* 현재 페이지 자료들의 유효(ACT) 첨부를 조회해 각 MaterialResDto에 그룹핑 (빈 목록이면 첨부 조회 생략 — IN() 방지) */
    private void attachMaterialAttachments(List<LmsStuMaterialsDto.MaterialResDto> materials) {
        if (materials.isEmpty()) {
            return;
        }
        List<Long> uploadIds = materials.stream()
                .map(LmsStuMaterialsDto.MaterialResDto::getUploadId)
                .collect(Collectors.toList());
        Map<Long, List<LmsStuMaterialsDto.AttachmentResDto>> grouped =
                lmsStuMaterialsMapper.selectActiveAttachmentsByUploadIds(uploadIds).stream()
                        .collect(Collectors.groupingBy(
                                LmsStuMaterialsDto.AttachmentRow::getUploadId,
                                Collectors.mapping(this::toAttachmentResDto, Collectors.toList())));
        for (LmsStuMaterialsDto.MaterialResDto material : materials) {
            material.setAttachments(grouped.getOrDefault(material.getUploadId(), Collections.emptyList()));
        }
    }

    private LmsStuMaterialsDto.LectureResDto toLectureResDto(LmsStuMaterialsDto.LectureRow row) {
        return LmsStuMaterialsDto.LectureResDto.builder()
                .lecId(row.getLecId())
                .courseName(row.getCourseName())
                .lecSection(row.getLecSection())
                .semYear(row.getSemYear())
                .semTerm(row.getSemTerm())
                .build();
    }

    private LmsStuMaterialsDto.MaterialResDto toMaterialResDto(LmsStuMaterialsDto.MaterialRow row) {
        return LmsStuMaterialsDto.MaterialResDto.builder()
                .uploadId(row.getUploadId())
                .lecUplTitle(row.getLecUplTitle())
                .lecUplContent(row.getLecUplContent())
                .lecUplRegDate(row.getLecUplRegDate())
                .attachments(Collections.emptyList())
                .downloadable(true)
                .lockedReason(null)
                .build();
    }

    private LmsStuMaterialsDto.AttachmentResDto toAttachmentResDto(LmsStuMaterialsDto.AttachmentRow row) {
        return LmsStuMaterialsDto.AttachmentResDto.builder()
                .attachmentId(row.getAttachmentId())
                .fileName(row.getFileName())
                .fileExt(row.getFileExt())
                .fileSize(row.getFileSize())
                .build();
    }

    private Long requireStudentLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsStuMaterialsMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }
}
