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

import java.util.List;

/**
 * PLM-005 / PLM-005-01 교수 강의 자료 업로드 DTO 묶음 (3분류 네스티드 — CLAUDE-BE §9 #12).
 * <p>매핑 row(LectureRow/MaterialRow/AttachmentRow/SemesterOptionRow/AttachmentDiskRow) + InsertParam(매퍼 쓰기 파라미터)
 * + 응답(LectureResDto/MaterialResDto/AttachmentResDto/SemesterOptionResDto/MetaResDto) + 요청(CreateReqDto/UpdateReqDto).
 * service가 매핑 row → ResDto 변환(첨부 조립 등). MyBatis resultType은 {@code ...LmsProfUploadDto$Inner} 형태.
 */
public final class LmsProfUploadDto {

    private LmsProfUploadDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType/parameterType, 내부 전용 ===== */

    /** 등록 폼 과목(강의) 드롭다운 1행 매핑 (selectLecturesByProfessor) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureRow {
        private Long lecId;
        private String courseName;   // LECTURE_CODE.LEC_COD_NAME
        private Integer lecSection;  // 분반
        private Integer year;        // SEMESTERS.SEM_YEAR
        private String termCode;     // SEMESTERS.SEM_TERM
        private String lecValStatus; // LECTURE.LEC_VAL_STATUS
    }

    /** 강의 자료 본체 1건 매핑 (selectUploadsPaged / selectUploadById) — 첨부는 service가 별도 조립 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialRow {
        private Long uploadId;     // LECTURE_UPLOADING.LEC_UPL_ID
        private Long lecId;
        private String courseName;
        private Integer lecSection; // LECTURE.LEC_SECTION 분반
        private Integer year;      // SEMESTERS.SEM_YEAR
        private String termCode;   // SEMESTERS.SEM_TERM
        private String title;
        private String content;    // Tiptap HTML 문자열(CLOB) — sanitize는 표시단(FE)
        private String uploadedAt; // "YYYY-MM-DD" (LEC_UPL_REG_DATE)
    }

    /** 유효(ACT) 첨부 1행 매핑 (selectActiveAttachments... — uploadId 포함, 서비스 그룹핑용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRow {
        private Long uploadId;
        private Long attachmentId;
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 자료 보유 학기 distinct 1행 매핑 (selectDistinctMaterialSemesters) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterOptionRow {
        private Integer year;
        private String termCode;
    }

    /** 삭제 시 디스크 파일 정리용 첨부 1행 매핑 (selectAttachmentsByUploadId) — 내부 전용 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDiskRow {
        private Long attachmentId;
        private String trnFileName; // 서버 저장 파일명
    }

    /** LECTURE_UPLOADING INSERT 파라미터 — uploadId는 selectKey(시퀀스)로 채번 후 채워짐 (FE 무관 매퍼 입출력) */
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

    /* ===== 응답(ResDto) — JSON 프로퍼티명 = FE 계약(불변) ===== */

    /** 등록 폼 과목(강의) 드롭다운 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureResDto {
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer year;
        private String termCode;
        private String lecValStatus;
    }

    /** 강의 자료 1건 응답 — 목록·등록/수정 공용. 첨부 ACT 전체 배열(없으면 빈 배열) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialResDto {
        private Long uploadId;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer year;
        private String termCode;
        private String title;
        private String content;
        private String uploadedAt;
        private List<AttachmentResDto> attachments;
    }

    /** 첨부 1건 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentResDto {
        private Long attachmentId; // 수정 시 개별 제거 식별자
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 목록 필터 옵션 학기 1건 응답 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SemesterOptionResDto {
        private Integer year;
        private String termCode;
    }

    /** 목록 메타 응답 — 전체 건수(필터 무관) + 필터 드롭다운 옵션(자료 보유 년도/학기) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetaResDto {
        private long totalAll;
        private List<SemesterOptionResDto> semesters;
    }

    /* ===== 요청(ReqDto) ===== */

    /** 등록 요청 (multipart/form-data, @ModelAttribute) — 파일 선택(텍스트 설명만으로도 등록 가능) */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateReqDto {
        @NotNull(message = "대상 강의를 선택해 주세요.")
        private Long lecId;

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 500, message = "제목은 500자 이내여야 합니다.") // DB LEC_UPL_TITLE VARCHAR2(500 CHAR)
        private String title;

        // FE 에디터 기준 텍스트 4000자 제한 — HTML(서식 포함)은 그보다 길어 넉넉한 상한만 가드(CLOB)
        @Size(max = 20000, message = "강의 설명이 너무 깁니다.")
        private String content;

        private List<MultipartFile> files; // 선택·다중 — 있을 때만 검증·저장
    }

    /** 수정 요청 — 강의(lecId) 변경 불가. files=추가할 새 파일들 / removeAttachmentIds=제거할 기존 첨부 ID */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateReqDto {
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 500, message = "제목은 500자 이내여야 합니다.")
        private String title;

        @Size(max = 20000, message = "강의 설명이 너무 깁니다.")
        private String content;

        private List<MultipartFile> files;          // 추가 첨부 (교체 아님 — 기존에 더해짐)
        private List<Long> removeAttachmentIds;     // 제거할 기존 첨부 ID
    }
}
