package com.univus.app.lms.service;

import com.univus.app.common.StorageService;
import com.univus.app.lms.dto.LmsProfNoticeDto;
import com.univus.app.lms.mapper.LmsProfNoticeMapper;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PLM-007 교수 공지사항 관리 ServiceImpl (PLM-005 강의 업로드 미러).
 * - 첨부 다중: 작성=여러 파일 / 수정=기존 유지 + 추가(files) + 개별 제거(removeAttachmentIds, ATT_VAL_STATUS='DEL' 소프트 무효화).
 * - 매퍼는 매핑 row 반환 → service가 ResDto 변환(첨부 조립). 검증 실패 = ResponseStatusException(400/403).
 * - 파일 저장 = StorageService(로컬 디스크) 재사용. 공지 첨부는 합계 100MB 제한(강의자료 5GB와 별도, FE와 동일).
 * - PLM-005와 차이: 목록 페이지네이션·메타 없음(선택 강의 공지 전체 최신순), 제목 200자, 작성자=담당교수.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsProfNoticeServiceImpl implements LmsProfNoticeService {

    private final LmsProfNoticeMapper lmsProfNoticeMapper;
    private final StorageService storageService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    // 저장 하위 폴더 / 웹 접근 경로 (강의자료와 동일 관례 — 인증 보호, permitAll 아님)
    private static final String ANNOUNCEMENT_SUBDIR = "lms" + File.separator + "professor" + File.separator + "announcement";
    private static final String ANNOUNCEMENT_URL_PREFIX = "/uploads/lms/professor/announcement/";

    // 공지 첨부 허용 확장자 — FE(lmsProfessorNoticeApi.ts NOTICE_ALLOWED_EXTS)와 동일(문서·이미지·압축, 영상/음성 제외).
    // 목록 변경 시 FE·BE 동시 수정 필요(자동 동기화 없음).
    private static final Set<String> ALLOWED_EXTS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "hwp", "hwpx", "txt", // 문서
            "png", "jpg", "jpeg", "gif",                                              // 이미지
            "zip");                                                                   // 압축
    private static final long MAX_TOTAL_SIZE = 100L * 1024 * 1024; // 첨부 합계 100MB (파일당 아님)
    private static final int MAX_FILENAME_LENGTH = 255;            // LEC_ANN_ATT_ORG_FIL_NAME VARCHAR2(255 CHAR)
    private static final int MAX_TITLE_LENGTH = 200;              // LEC_ANN_TITLE VARCHAR2(200 CHAR)

    @Override
    @Transactional(readOnly = true)
    public List<LmsProfNoticeDto.LectureResDto> getLectures(Long memberId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        return lmsProfNoticeMapper.selectLecturesByProfessor(lmsPrfId).stream()
                .map(this::toLectureResDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LmsProfNoticeDto.NoticeResDto> getNotices(Long memberId, Long lecId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        List<LmsProfNoticeDto.NoticeResDto> notices =
                lmsProfNoticeMapper.selectNoticesByLecture(lecId, lmsPrfId).stream()
                        .map(this::toNoticeResDto)
                        .collect(Collectors.toList());
        attachNoticeAttachments(notices);
        return notices;
    }

    /* 공지 목록 첨부를 한 번에 조회해 그룹핑 (빈 목록이면 IN() 방지 위해 생략) */
    private void attachNoticeAttachments(List<LmsProfNoticeDto.NoticeResDto> notices) {
        if (notices.isEmpty()) {
            return;
        }
        List<Long> noticeIds = notices.stream()
                .map(LmsProfNoticeDto.NoticeResDto::getNoticeId)
                .collect(Collectors.toList());
        Map<Long, List<LmsProfNoticeDto.AttachmentResDto>> grouped =
                lmsProfNoticeMapper.selectActiveAttachmentsByNoticeIds(noticeIds).stream()
                        .collect(Collectors.groupingBy(
                                LmsProfNoticeDto.AttachmentRow::getNoticeId,
                                Collectors.mapping(this::toAttachmentResDto, Collectors.toList())));
        for (LmsProfNoticeDto.NoticeResDto notice : notices) {
            notice.setAttachments(grouped.getOrDefault(notice.getNoticeId(), Collections.emptyList()));
        }
    }

    @Override
    @Transactional
    public LmsProfNoticeDto.NoticeResDto createNotice(Long memberId, LmsProfNoticeDto.CreateReqDto request) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);

        String title = requireTitle(request.getLecAnnTitle());
        List<MultipartFile> files = normalizeFiles(request.getFiles());
        validateFiles(files);

        if (lmsProfNoticeMapper.countOwnedLecture(request.getLecId(), lmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 담당하는 강의가 아닙니다.");
        }

        LmsProfNoticeDto.InsertParam param = LmsProfNoticeDto.InsertParam.builder()
                .lecId(request.getLecId())
                .title(title)
                .content(normalizeContent(request.getLecAnnContent()))
                .build();
        lmsProfNoticeMapper.insertNotice(param);

        for (MultipartFile file : files) {
            saveAttachment(param.getNoticeId(), file);
        }

        log.info("공지 작성 lmsPrfId={} noticeId={} lecId={} files={}",
                lmsPrfId, param.getNoticeId(), request.getLecId(), files.size());
        return findNotice(param.getNoticeId());
    }

    @Override
    @Transactional
    public LmsProfNoticeDto.NoticeResDto updateNotice(Long memberId, Long noticeId, LmsProfNoticeDto.UpdateReqDto request) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        requireOwnedNotice(noticeId, lmsPrfId);

        String title = requireTitle(request.getLecAnnTitle());
        lmsProfNoticeMapper.updateNotice(noticeId, title, normalizeContent(request.getLecAnnContent()));

        // 기존 첨부 개별 제거 (소프트 무효화 — noticeId 조건으로 본 공지의 첨부만)
        List<Long> removeIds = request.getRemoveAttachmentIds() == null
                ? Collections.emptyList()
                : request.getRemoveAttachmentIds();
        for (Long attachmentId : removeIds) {
            lmsProfNoticeMapper.invalidateAttachmentById(noticeId, attachmentId);
        }

        // 새 파일 추가 (교체 아님 — 기존 유효 첨부에 더해짐). 합계 검증은 신규분 기준(기존 유지분은 이미 저장됨)
        List<MultipartFile> files = normalizeFiles(request.getFiles());
        validateFiles(files);
        for (MultipartFile file : files) {
            saveAttachment(noticeId, file);
        }

        log.info("공지 수정 lmsPrfId={} noticeId={} addedFiles={} removedAttachments={}",
                lmsPrfId, noticeId, files.size(), removeIds.size());
        return findNotice(noticeId);
    }

    @Override
    @Transactional
    public void deleteNotice(Long memberId, Long noticeId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        requireOwnedNotice(noticeId, lmsPrfId);

        // 디스크 정리 대상 확보 후 DB 삭제 (첨부 → 본체, FK 순서)
        List<LmsProfNoticeDto.AttachmentDiskRow> attachments =
                lmsProfNoticeMapper.selectAttachmentsByNoticeId(noticeId);
        lmsProfNoticeMapper.deleteAttachments(noticeId);
        lmsProfNoticeMapper.deleteNotice(noticeId);

        // 디스크 파일 정리 — 실패해도 DB 삭제는 유지(고아 파일 허용, 로그만)
        String directoryPath = uploadRoot + File.separator + ANNOUNCEMENT_SUBDIR;
        for (LmsProfNoticeDto.AttachmentDiskRow attachment : attachments) {
            boolean deleted = storageService.deleteFile(directoryPath, attachment.getTrnFileName());
            if (!deleted) {
                log.warn("공지 디스크 파일 삭제 실패(고아 파일) noticeId={} trnFileName={}",
                        noticeId, attachment.getTrnFileName());
            }
        }
        log.info("공지 삭제 lmsPrfId={} noticeId={} attachments={}", lmsPrfId, noticeId, attachments.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId) {
        Long lmsPrfId = requireProfessorLmsPrfId(memberId);
        LmsProfNoticeDto.AttachmentDownloadRow file =
                lmsProfNoticeMapper.selectOwnedActiveAttachment(attachmentId, lmsPrfId);
        if (file == null) {
            // 없음·타 교수 강의·무효(DEL) 모두 404 (존재 노출 방지)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부 파일을 찾을 수 없습니다.");
        }
        String directoryPath = uploadRoot + File.separator + ANNOUNCEMENT_SUBDIR;
        log.info("공지 첨부 다운로드 lmsPrfId={} attachmentId={}", lmsPrfId, attachmentId);
        return storageService.downloadFile(directoryPath, file.getTrnFileName(), file.getFileName());
    }

    /* 공지 단건 + 유효 첨부 조립 */
    private LmsProfNoticeDto.NoticeResDto findNotice(Long noticeId) {
        LmsProfNoticeDto.NoticeRow row = lmsProfNoticeMapper.selectNoticeById(noticeId);
        if (row == null) {
            return null;
        }
        LmsProfNoticeDto.NoticeResDto notice = toNoticeResDto(row);
        notice.setAttachments(
                lmsProfNoticeMapper.selectActiveAttachmentsByNoticeId(noticeId).stream()
                        .map(this::toAttachmentResDto)
                        .collect(Collectors.toList()));
        return notice;
    }

    /* ===== 매핑 row → 응답 ResDto 변환 ===== */

    private LmsProfNoticeDto.LectureResDto toLectureResDto(LmsProfNoticeDto.LectureRow row) {
        return LmsProfNoticeDto.LectureResDto.builder()
                .lecId(row.getLecId())
                .courseName(row.getCourseName())
                .lecSection(row.getLecSection())
                .semYear(row.getSemYear())
                .semTerm(row.getSemTerm())
                .build();
    }

    private LmsProfNoticeDto.NoticeResDto toNoticeResDto(LmsProfNoticeDto.NoticeRow row) {
        return LmsProfNoticeDto.NoticeResDto.builder()
                .noticeId(row.getNoticeId())
                .lecId(row.getLecId())
                .courseName(row.getCourseName())
                .lecSection(row.getLecSection())
                .semYear(row.getSemYear())
                .semTerm(row.getSemTerm())
                .lecAnnTitle(row.getLecAnnTitle())
                .lecAnnContent(row.getLecAnnContent())
                .author(row.getAuthor())
                .lecAnnRegDate(row.getLecAnnRegDate())
                .listDate(row.getListDate())
                .build();
    }

    private LmsProfNoticeDto.AttachmentResDto toAttachmentResDto(LmsProfNoticeDto.AttachmentRow row) {
        return LmsProfNoticeDto.AttachmentResDto.builder()
                .attachmentId(row.getAttachmentId())
                .fileName(row.getFileName())
                .fileSize(row.getFileSize())
                .build();
    }

    /* 파일 저장(디스크) + 첨부 메타 INSERT */
    private void saveAttachment(Long noticeId, MultipartFile file) {
        String directoryPath = uploadRoot + File.separator + ANNOUNCEMENT_SUBDIR;
        String trnFileName = storageService.uploadFileToServer(file, directoryPath);
        String orgFileName = file.getOriginalFilename();
        String orgUrl = ANNOUNCEMENT_URL_PREFIX + trnFileName;
        String extType = extractExtension(orgFileName);
        lmsProfNoticeMapper.insertAttachment(
                noticeId, orgFileName, trnFileName, orgUrl, file.getSize(), extType);
    }

    /* 교수 LMS 프로필 확인 — 없으면 403 (PLM-003/004/005와 동일 패턴) */
    private Long requireProfessorLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsProfNoticeMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    /* 공지 소유권 확인 — 본인 강의 공지 아니면 403 */
    private void requireOwnedNotice(Long noticeId, Long lmsPrfId) {
        if (lmsProfNoticeMapper.countOwnedNotice(noticeId, lmsPrfId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 공지가 아닙니다.");
        }
    }

    /* 제목 검증 — trim 후 필수·200자 (DTO @Valid와 별개로 400 보장) */
    private String requireTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해 주세요.");
        }
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목은 " + MAX_TITLE_LENGTH + "자 이내여야 합니다.");
        }
        return trimmed;
    }

    /* 내용 정규화 — 빈 문자열/공백뿐이면 null 저장 (CLOB NULL 허용. sanitize는 표시단 FE 책임) */
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

    /* 파일 검증 — 단일 파일(확장자 화이트리스트/파일명 255자) + 요청 합계 100MB (FE와 동일 기준) */
    private void validateFiles(List<MultipartFile> files) {
        long total = 0;
        for (MultipartFile file : files) {
            validateFile(file);
            total += file.getSize();
        }
        if (total > MAX_TOTAL_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "첨부 파일 전체 합계는 100MB를 초과할 수 없습니다.");
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
                    "허용되지 않는 파일 형식입니다. 문서(PDF·HWP·DOC·PPT·XLS·TXT)·이미지(JPG·PNG·GIF)·ZIP만 첨부할 수 있습니다.");
        }
    }

    /* 파일명 → 소문자 확장자 ("notice.PDF" → "pdf", 확장자 없으면 "") */
    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1).toLowerCase();
    }
}
