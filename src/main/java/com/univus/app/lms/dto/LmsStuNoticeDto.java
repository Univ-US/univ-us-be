package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * SLM-009 학생 공지사항 열람 DTO (과목 선택 → 서버 페이지네이션).
 * <p>본체=LECTURE_ANNOUNCEMENT, 첨부=LECTURE_ANNOUNCEMENT_ATTACHMENT. 읽기 전용.
 * <p>과목(년도/학기/과목명)은 lectures 드롭다운 응답이 담당 — 공지 본체 응답은 본문·작성자·첨부만.
 */
public final class LmsStuNoticeDto {

    private LmsStuNoticeDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType, 내부 전용 ===== */

    /** 수강 과목(강의) 드롭다운 1행 매핑 (selectEnrolledLectures) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureRow {
        private Long lecId;
        private String courseName;   // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;  // LECTURE.LEC_SECTION (조인, 분반)
        private Integer semYear;     // SEMESTERS.SEM_YEAR (조인)
        private String semTerm;      // SEMESTERS.SEM_TERM (조인)
    }

    /** 공지 본체 1건 매핑 (selectNoticesPaged) — 첨부는 service가 별도 조립 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeRow {
        private Long noticeId;        // LECTURE_ANNOUNCEMENT.LEC_ANN_ID (PK → 의미명)
        private String lecAnnTitle;   // LECTURE_ANNOUNCEMENT.LEC_ANN_TITLE (자기 컬럼)
        private String lecAnnContent; // LECTURE_ANNOUNCEMENT.LEC_ANN_CONTENT (자기 컬럼, Tiptap HTML CLOB)
        private String author;        // 작성 교수 MEMBER_NAME (조인 → 의미별칭)
        private String authorImageUrl;// LMS_PROFILE_IMAGE.LMS_PRF_IMG_ORG_URL (서브쿼리 → 의미별칭)
        private Long professorLmsPrfId;// LECTURE.LMS_PRF_ID 작성 교수 (FE lmsAvatar 색 시드 → 의미명)
        private String lecAnnRegDate; // LEC_ANN_REG_DATE "YYYY-MM-DD HH:mm" (자기 컬럼)
        private String listDate;      // 목록 축약 "MM.DD" (LEC_ANN_REG_DATE 파생 → 의미명)
    }

    /** 유효(ACT) 첨부 1행 매핑 (selectActiveAttachmentsByNoticeIds — noticeId 포함, 서비스 그룹핑용) */
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

    /** 첨부 다운로드 1행 매핑 (selectOwnedActiveAttachment) — 내부 전용 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDownloadRow {
        private String trnFileName;
        private String fileName;
    }

    /* ===== 응답(ResDto) ===== */

    /** 수강 과목(강의) 드롭다운 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureResDto {
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer semYear;     // SEMESTERS.SEM_YEAR
        private String semTerm;      // SEMESTERS.SEM_TERM
    }

    /** 첨부 1건 응답 — downloadUrl은 service가 합성 */
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

    /** 공지 1건 응답 — 본문·작성자·첨부 (과목은 lectures 드롭다운이 담당) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeResDto {
        private Long noticeId;        // LEC_ANN_ID (PK → 의미명)
        private String lecAnnTitle;   // LEC_ANN_TITLE
        private String author;        // 작성 교수 MEMBER_NAME (조인)
        private String authorImageUrl;
        private Long professorLmsPrfId; // 작성 교수 LMS_PRF_ID (FE lmsAvatar 색 시드)
        private String lecAnnRegDate; // 등록일시 "YYYY-MM-DD HH:mm" (LEC_ANN_REG_DATE)
        private String listDate;      // 좌측 목록 축약 날짜 "MM.DD" (REG_DATE 파생)
        private String lecAnnContent; // LEC_ANN_CONTENT
        private List<NoticeAttachmentResDto> attachments;
    }
}
