package com.univus.app.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * PLM-005 / PLM-005-01 교수 강의 자료 업로드 DTO 묶음.
 * 응답·요청 DTO + MyBatis 매핑용 row DTO를 한 파일에 중첩으로 모음.
 * (MyBatis resultType은 com.univus.app.lms.dto.LmsUploadDto$Inner 형태로 참조)
 */
public final class LmsUploadDto {

    private LmsUploadDto() {
    }

    /** 등록 폼 과목(강의) 드롭다운 1행 — 교수 담당 강의(분반·학기 포함, 라벨 매핑은 FE 공통코드) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Lecture {
        private Long lecId;
        private String courseName;   // LECTURE_CODE.LEC_COD_NAME
        private Integer lecSection;  // 분반
        private Integer year;        // SEMESTERS.SEM_YEAR
        private String termCode;     // SEMESTERS.SEM_TERM (공통코드 SEM_TERM)
        private String lecValStatus; // LECTURE.LEC_VAL_STATUS (공통코드 LEC_VAL_STATUS)
    }

    /** 강의 자료 1건 — 목록·등록/수정 응답 공용. 첨부는 유효(ACT) 전체를 배열로 노출(다중 첨부, 없으면 빈 배열) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Material {
        private Long uploadId;     // LECTURE_UPLOADING.LEC_UPL_ID
        private Long lecId;
        private String courseName;
        private Integer year;      // SEMESTERS.SEM_YEAR — 목록 년도/학기 필터용 (2026-06-11 추가)
        private String termCode;   // SEMESTERS.SEM_TERM (SM1/SMR/SM2/WNT) — 라벨은 FE 공통코드 매핑
        private String title;
        private String content;    // 강의 설명 (Tiptap HTML 문자열, CLOB) — sanitize는 표시단(FE) 책임
        private String uploadedAt; // "YYYY-MM-DD" (LEC_UPL_REG_DATE)
        private java.util.List<Attachment> attachments; // 유효 첨부 전체 (등록순)
    }

    /** 첨부 1건 (응답용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attachment {
        private Long attachmentId; // LEC_UPL_ATT_ID — 수정 시 개별 제거 식별자
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 첨부 목록 조회 row (uploadId 포함 — 서비스에서 자료별 그룹핑용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentListRow {
        private Long uploadId;
        private Long attachmentId;
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 등록 요청 (multipart/form-data, @ModelAttribute 바인딩) — 파일 선택(텍스트 설명만으로도 등록 가능) */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "대상 강의를 선택해 주세요.")
        private Long lecId;

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 500, message = "제목은 500자 이내여야 합니다.") // DB LEC_UPL_TITLE VARCHAR2(500 CHAR)
        private String title;

        // FE 에디터 기준 텍스트 4000자 제한 — HTML(서식 포함)은 그보다 길어 넉넉한 상한만 가드(CLOB)
        @Size(max = 20000, message = "강의 설명이 너무 깁니다.")
        private String content;

        private java.util.List<MultipartFile> files; // 선택·다중 — 있을 때만 검증·저장 (텍스트만 등록 가능)
    }

    /** 수정 요청 — 강의(lecId) 변경 불가(FE 수정 모달 disabled).
     *  첨부(다중): files = 추가할 새 파일들(기존 유지+추가) / removeAttachmentIds = 제거할 기존 첨부 ID들 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 500, message = "제목은 500자 이내여야 합니다.")
        private String title;

        @Size(max = 20000, message = "강의 설명이 너무 깁니다.")
        private String content;

        private java.util.List<MultipartFile> files; // 추가 첨부 (교체 아님 — 기존에 더해짐)

        private java.util.List<Long> removeAttachmentIds; // 제거할 기존 첨부 ID
    }

    /** LECTURE_UPLOADING INSERT 파라미터 — uploadId는 selectKey(시퀀스)로 채번 후 채워짐 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InsertParam {
        private Long uploadId;
        private Long lecId;
        private String title;
        private String content;
    }

    /** 첨부 1행 (삭제 시 디스크 파일 정리용 조회) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRow {
        private Long attachmentId;
        private String trnFileName; // 서버 저장 파일명
    }
}
