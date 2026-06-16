package com.univus.app.lms.service;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LmsStuMaterialsServiceImpl implements LmsStuMaterialsService {

    private static final Map<String, String> TERM_LABELS = Map.of(
            "SM1", "1학기",
            "SMR", "여름 계절",
            "SM2", "2학기",
            "WNT", "겨울 계절");

    private static final String MATERIAL_SUBDIR = "lms" + File.separator + "professor" + File.separator + "material";

    private final LmsStuMaterialsMapper lmsStuMaterialsMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuMaterialsDto.SemesterMaterialsResDto> getMaterials(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        List<LmsStuMaterialsDto.MaterialFlatRow> rows = lmsStuMaterialsMapper.selectMaterialRows(lmsPrfId);

        Map<String, SemesterAccumulator> semesters = new LinkedHashMap<>();
        Map<Long, LmsStuMaterialsDto.MaterialResDto> materialIndex = new LinkedHashMap<>();

        for (LmsStuMaterialsDto.MaterialFlatRow row : rows) {
            String key = row.getYear() + ":" + row.getTermCode();
            SemesterAccumulator semester = semesters.computeIfAbsent(
                    key,
                    ignored -> new SemesterAccumulator(row.getYear(), row.getTermCode()));
            CourseAccumulator course = semester.course(row);
            if (row.getUploadId() != null) {
                LmsStuMaterialsDto.MaterialResDto material = toMaterialResDto(row);
                course.add(material);
                materialIndex.put(material.getUploadId(), material);
            }
        }

        attachMaterialAttachments(materialIndex);

        return semesters.values().stream()
                .map(SemesterAccumulator::toResponse)
                .collect(Collectors.toList());
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

    private void attachMaterialAttachments(Map<Long, LmsStuMaterialsDto.MaterialResDto> materialIndex) {
        if (materialIndex.isEmpty()) {
            return;
        }

        Map<Long, List<LmsStuMaterialsDto.AttachmentResDto>> grouped =
                lmsStuMaterialsMapper.selectActiveAttachmentsByUploadIds(new ArrayList<>(materialIndex.keySet())).stream()
                        .collect(Collectors.groupingBy(
                                LmsStuMaterialsDto.AttachmentRow::getUploadId,
                                Collectors.mapping(this::toAttachmentResDto, Collectors.toList())));

        for (Map.Entry<Long, LmsStuMaterialsDto.MaterialResDto> entry : materialIndex.entrySet()) {
            entry.getValue().setAttachments(grouped.getOrDefault(entry.getKey(), Collections.emptyList()));
        }
    }

    private LmsStuMaterialsDto.MaterialResDto toMaterialResDto(LmsStuMaterialsDto.MaterialFlatRow row) {
        return LmsStuMaterialsDto.MaterialResDto.builder()
                .uploadId(row.getUploadId())
                .title(row.getTitle())
                .content(row.getContent())
                .uploadedAt(row.getUploadedAt())
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

    private static String semesterLabel(Integer year, String termCode) {
        return year + "년 " + TERM_LABELS.getOrDefault(termCode, termCode);
    }

    private static class SemesterAccumulator {
        private final Integer year;
        private final String termCode;
        private final Map<Long, CourseAccumulator> courses = new LinkedHashMap<>();

        private SemesterAccumulator(Integer year, String termCode) {
            this.year = year;
            this.termCode = termCode;
        }

        private CourseAccumulator course(LmsStuMaterialsDto.MaterialFlatRow row) {
            return courses.computeIfAbsent(row.getLecId(), ignored -> new CourseAccumulator(row));
        }

        private LmsStuMaterialsDto.SemesterMaterialsResDto toResponse() {
            return LmsStuMaterialsDto.SemesterMaterialsResDto.builder()
                    .year(year)
                    .termCode(termCode)
                    .semesterLabel(semesterLabel(year, termCode))
                    .courses(courses.values().stream()
                            .map(CourseAccumulator::toResponse)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    private static class CourseAccumulator {
        private final Long lecId;
        private final String courseName;
        private final Integer lecSection;
        private final List<LmsStuMaterialsDto.MaterialResDto> materials = new ArrayList<>();

        private CourseAccumulator(LmsStuMaterialsDto.MaterialFlatRow row) {
            this.lecId = row.getLecId();
            this.courseName = row.getCourseName();
            this.lecSection = row.getLecSection();
        }

        private void add(LmsStuMaterialsDto.MaterialResDto material) {
            materials.add(material);
        }

        private LmsStuMaterialsDto.CourseMaterialsResDto toResponse() {
            return LmsStuMaterialsDto.CourseMaterialsResDto.builder()
                    .lecId(lecId)
                    .courseName(courseName)
                    .lecSection(lecSection)
                    .materials(materials)
                    .build();
        }
    }
}
