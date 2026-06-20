package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** SLM-006 학생 강의 자료 DTO (과목 선택 → 서버 페이지네이션). */
public final class LmsStuMaterialsDto {

    private LmsStuMaterialsDto() {
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

    /** 강의 자료 본체 1건 매핑 (selectMaterialsPaged) — 첨부는 service가 별도 조립 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialRow {
        private Long uploadId;        // LECTURE_UPLOADING.LEC_UPL_ID (자기 PK — 의미명 유지)
        private String lecUplTitle;   // LECTURE_UPLOADING.LEC_UPL_TITLE
        private String lecUplContent; // LECTURE_UPLOADING.LEC_UPL_CONTENT (Tiptap HTML CLOB — sanitize는 표시단 FE)
        private String lecUplRegDate; // LECTURE_UPLOADING.LEC_UPL_REG_DATE "YYYY-MM-DD"
    }

    /** 유효(ACT) 첨부 1행 매핑 (selectActiveAttachmentsByUploadIds — uploadId 포함, 서비스 그룹핑용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRow {
        private Long uploadId;
        private Long attachmentId;  // LEC_UPL_ATT_ID (자기 PK — 의미명 유지)
        private String fileName;    // 첨부 파일 메타 — 의미명 유지(표시·다운로드용)
        private String fileExt;
        private Long fileSize;
    }

    /** 첨부 다운로드 1행 매핑 (selectDownloadAttachment) — 내부 전용 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDownloadRow {
        private Long attachmentId;
        private String fileName;
        private String trnFileName; // 서버 저장 파일명 (파일 메타 — 의미명 유지)
    }

    /* ===== 응답(ResDto) — JSON 프로퍼티명 = DB 컬럼 카멜(조인/파생=의미별칭) ===== */

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

    /** 첨부 1건 응답 — 파일 메타는 의미명 유지 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentResDto {
        private Long attachmentId;
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 강의 자료 1건 응답 — 첨부 ACT 전체 배열(없으면 빈 배열) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialResDto {
        private Long uploadId;
        private String lecUplTitle;   // LECTURE_UPLOADING.LEC_UPL_TITLE
        private String lecUplContent; // LECTURE_UPLOADING.LEC_UPL_CONTENT
        private String lecUplRegDate; // LECTURE_UPLOADING.LEC_UPL_REG_DATE "YYYY-MM-DD"
        private List<AttachmentResDto> attachments;
        private boolean downloadable;     // 파생
        private String lockedReason;      // 파생
    }
}
