package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * SLM-009 학생 공지사항 열람 DTO 묶음 (3분류 네스티드 — CLAUDE-BE §9 #12).
 * <p>본체=LECTURE_ANNOUNCEMENT, 첨부=LECTURE_ANNOUNCEMENT_ATTACHMENT. PLM-007 교수 공지 BE 미러(읽기 전용).
 * <p>※ 명명 규칙: DTO 변수명 = DB 컬럼명 카멜(자기/앵커 LECTURE_ANNOUNCEMENT·SEMESTERS), 조인/파생은 의미 별칭 — PLM-007 정본 일관.
 *    응답 JSON = 컬럼 카멜(lecAnnTitle·lecAnnContent·lecAnnRegDate·semYear·semTerm).
 *    PK noticeId·조인 의미명(courseName·courseFullName·author·authorImageUrl)·파일 메타(fileName·fileSize)·파생(semesterLabel·listDate·featured·downloadUrl)은 의미명 유지.
 */
public final class LmsStuNoticeDto {

    private LmsStuNoticeDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeRow {
        private Long noticeId;        // LECTURE_ANNOUNCEMENT.LEC_ANN_ID (PK → 의미명)
        private Integer semYear;      // SEMESTERS.SEM_YEAR (조인)
        private String semTerm;       // SEMESTERS.SEM_TERM (조인)
        private Long lecId;
        private Integer lecSection;   // LECTURE.LEC_SECTION
        private String courseName;    // LECTURE_CODE.LEC_COD_NAME (조인 → 의미별칭)
        private String courseFullName;// LECTURE_CODE.LEC_COD_NAME (조인 → 의미별칭)
        private String lecAnnTitle;   // LECTURE_ANNOUNCEMENT.LEC_ANN_TITLE (자기 컬럼)
        private String lecAnnContent; // LECTURE_ANNOUNCEMENT.LEC_ANN_CONTENT (자기 컬럼, Tiptap HTML CLOB)
        private String author;        // 작성 교수 MEMBER_NAME (조인 → 의미별칭)
        private String authorImageUrl;// LMS_PROFILE_IMAGE.LMS_PRF_IMG_ORG_URL (서브쿼리 → 의미별칭)
        private Long professorLmsPrfId;// LECTURE.LMS_PRF_ID 작성 교수 (FE lmsAvatar 색 시드 → 의미명)
        private String lecAnnRegDate; // LECTURE_ANNOUNCEMENT.LEC_ANN_REG_DATE "YYYY-MM-DD HH:mm" (자기 컬럼, alias는 예약어 'date' 회피)
        private String listDate;      // 목록 축약 "MM.DD" (LEC_ANN_REG_DATE 파생 → 의미명)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRow {
        private Long noticeId;
        private Long attachmentId;
        private String fileName;
        private Long fileSize;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDownloadRow {
        private String trnFileName;
        private String fileName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeAttachmentResDto {
        private Long attachmentId;
        private String fileName;
        private Long fileSize;
        private String downloadUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeResDto {
        private Long noticeId;        // LEC_ANN_ID (PK → 의미명)
        private Integer semYear;      // SEMESTERS.SEM_YEAR
        private String semTerm;       // SEMESTERS.SEM_TERM
        private String semesterLabel; // 파생 라벨 (의미명 유지)
        private Long lecId;
        private Integer lecSection;
        private String courseName;
        private String courseFullName;
        private String lecAnnTitle;   // LEC_ANN_TITLE
        private String author;        // 작성 교수 MEMBER_NAME (조인)
        private String authorImageUrl;
        private Long professorLmsPrfId; // 작성 교수 LMS_PRF_ID (FE lmsAvatar 색 시드)
        private String lecAnnRegDate; // 등록일시 "YYYY-MM-DD HH:mm" (LEC_ANN_REG_DATE)
        private String listDate;      // 좌측 목록 축약 날짜 "MM.DD" (REG_DATE 파생)
        private Boolean featured;     // 파생(현재 항상 false) — 데드필드 유지
        private String lecAnnContent; // LEC_ANN_CONTENT
        private List<NoticeAttachmentResDto> attachments;
    }
}
