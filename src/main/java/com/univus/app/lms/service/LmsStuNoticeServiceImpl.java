package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.PaginateUtilRestApiRes;
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

/**
 * SLM-009 학생 공지사항 열람 ServiceImpl.
 * 화면 = 수강 과목 1개 선택 → 그 과목 공지를 서버 페이지네이션(클라 slice 없음, 교수 PLM-005 미러).
 * 소유권: 본인 수강(DRP 제외) 강의의 공지만 — 매퍼 ENROLLMENT INNER JOIN으로 IDOR 차단.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsStuNoticeServiceImpl implements LmsStuNoticeService {

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
    public List<LmsStuNoticeDto.LectureResDto> getLectures(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        return lmsStuNoticeMapper.selectEnrolledLectures(lmsPrfId).stream()
                .map(this::toLectureResDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginateUtilRestApiRes<LmsStuNoticeDto.NoticeResDto> getNotices(
            Long memberId, Long lecId, int page, int size) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        long total = lmsStuNoticeMapper.countNotices(lmsPrfId, lecId);
        List<LmsStuNoticeDto.NoticeResDto> notices = lmsStuNoticeMapper.selectNoticesPaged(
                        lmsPrfId, lecId, PaginateUtilRestApi.offset(safePage, safeSize), safeSize).stream()
                .map(this::toNoticeResDto)
                .collect(Collectors.toList());
        attachNoticeAttachments(notices);
        log.info("학생 공지 조회 lmsPrfId={} lecId={} page={} size={} total={}", lmsPrfId, lecId, safePage, safeSize, total);
        return PaginateUtilRestApi.of(notices, total, safePage, safeSize);
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
        log.info("학생 공지 첨부 다운로드 lmsPrfId={} attachmentId={}", lmsPrfId, attachmentId);
        return storageService.downloadFile(directoryPath, file.getTrnFileName(), file.getFileName());
    }

    /* 현재 페이지 공지들의 유효(ACT) 첨부를 조회해 각 NoticeResDto에 그룹핑 (빈 목록이면 첨부 조회 생략) */
    private void attachNoticeAttachments(List<LmsStuNoticeDto.NoticeResDto> notices) {
        if (notices.isEmpty()) {
            return;
        }
        List<Long> noticeIds = notices.stream()
                .map(LmsStuNoticeDto.NoticeResDto::getNoticeId)
                .collect(Collectors.toList());
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

    private LmsStuNoticeDto.LectureResDto toLectureResDto(LmsStuNoticeDto.LectureRow row) {
        return LmsStuNoticeDto.LectureResDto.builder()
                .lecId(row.getLecId())
                .courseName(row.getCourseName())
                .lecSection(row.getLecSection())
                .semYear(row.getSemYear())
                .semTerm(row.getSemTerm())
                .build();
    }

    private LmsStuNoticeDto.NoticeResDto toNoticeResDto(LmsStuNoticeDto.NoticeRow row) {
        return LmsStuNoticeDto.NoticeResDto.builder()
                .noticeId(row.getNoticeId())
                .lecAnnTitle(row.getLecAnnTitle())
                .author(formatAuthor(row.getAuthor()))
                .authorImageUrl(row.getAuthorImageUrl())
                .professorLmsPrfId(row.getProfessorLmsPrfId())
                .lecAnnRegDate(row.getLecAnnRegDate())
                .listDate(row.getListDate())
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

    private static String formatAuthor(String author) {
        if (author == null || author.isBlank()) {
            return "교수";
        }
        return author.endsWith("교수") ? author : author + " 교수";
    }
}
