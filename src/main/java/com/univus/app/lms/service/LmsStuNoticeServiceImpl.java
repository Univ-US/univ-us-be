package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.lms.dto.LmsStuNoticeDto;
import com.univus.app.lms.mapper.LmsStuNoticeMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class LmsStuNoticeServiceImpl implements LmsStuNoticeService {

    private static final Map<String, String> TERM_LABELS = Map.of(
            "SM1", "1학기",
            "SMR", "여름 계절",
            "SM2", "2학기",
            "WNT", "겨울 계절");
    private static final String ANNOUNCEMENT_SUBDIR =
            "lms" + File.separator + "professor" + File.separator + "announcement";
    private static final String ATTACHMENT_DOWNLOAD_PATH =
            "/api/lms/student/notices/attachments/%d/file";

    private final LmsStuNoticeMapper lmsStuNoticeMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuNoticeDto.NoticeResDto> getNotices(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        List<LmsStuNoticeDto.NoticeResDto> notices = lmsStuNoticeMapper.selectStudentNotices(lmsPrfId)
                .stream()
                .map(this::toNoticeResDto)
                .toList();
        attachNoticeAttachments(notices);
        return notices;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        LmsStuNoticeDto.AttachmentDownloadRow file =
                lmsStuNoticeMapper.selectOwnedActiveAttachment(attachmentId, lmsPrfId);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공지 첨부 파일을 찾을 수 없습니다.");
        }

        String directoryPath = uploadRoot + File.separator + ANNOUNCEMENT_SUBDIR;
        log.info("Student notice attachment download lmsPrfId={} attachmentId={}", lmsPrfId, attachmentId);
        return storageService.downloadFile(directoryPath, file.getTrnFileName(), file.getFileName());
    }

    private void attachNoticeAttachments(List<LmsStuNoticeDto.NoticeResDto> notices) {
        if (notices.isEmpty()) {
            return;
        }

        List<Long> noticeIds = notices.stream()
                .map(LmsStuNoticeDto.NoticeResDto::getNoticeId)
                .toList();
        Map<Long, List<LmsStuNoticeDto.NoticeAttachmentResDto>> grouped =
                lmsStuNoticeMapper.selectActiveAttachmentsByNoticeIds(noticeIds).stream()
                        .collect(Collectors.groupingBy(
                                LmsStuNoticeDto.AttachmentRow::getNoticeId,
                                Collectors.mapping(this::toAttachmentResDto, Collectors.toList())));

        for (LmsStuNoticeDto.NoticeResDto notice : notices) {
            notice.setAttachments(grouped.getOrDefault(notice.getNoticeId(), Collections.emptyList()));
        }
    }

    private Long requireStudentLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsStuNoticeMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    private LmsStuNoticeDto.NoticeResDto toNoticeResDto(LmsStuNoticeDto.NoticeRow row) {
        return LmsStuNoticeDto.NoticeResDto.builder()
                .noticeId(row.getNoticeId())
                .semYear(row.getSemYear())
                .semTerm(row.getSemTerm())
                .semesterLabel(semesterLabel(row.getSemYear(), row.getSemTerm()))
                .lecId(row.getLecId())
                .lecSection(row.getLecSection())
                .courseName(row.getCourseName())
                .courseFullName(row.getCourseFullName() == null ? row.getCourseName() : row.getCourseFullName())
                .lecAnnTitle(row.getLecAnnTitle())
                .author(formatAuthor(row.getAuthor()))
                .authorImageUrl(row.getAuthorImageUrl())
                .lecAnnRegDate(row.getLecAnnRegDate())
                .listDate(row.getListDate())
                .featured(false)
                .lecAnnContent(row.getLecAnnContent())
                .attachments(Collections.emptyList())
                .build();
    }

    private LmsStuNoticeDto.NoticeAttachmentResDto toAttachmentResDto(LmsStuNoticeDto.AttachmentRow row) {
        return LmsStuNoticeDto.NoticeAttachmentResDto.builder()
                .attachmentId(row.getAttachmentId())
                .fileName(row.getFileName())
                .fileSize(row.getFileSize())
                .downloadUrl(String.format(ATTACHMENT_DOWNLOAD_PATH, row.getAttachmentId()))
                .build();
    }

    private static String semesterLabel(Integer year, String termCode) {
        return year + "년 " + TERM_LABELS.getOrDefault(termCode, termCode);
    }

    private static String formatAuthor(String author) {
        if (author == null || author.isBlank()) {
            return "교수";
        }
        return author.endsWith("교수") ? author : author + " 교수";
    }
}
