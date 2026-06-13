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
 * PLM-006 교수 과제 관리 DTO 묶음.
 * 과제(LECTURE_ASSIGNMENT) CRUD + 첨부(LECTURE_ASSIGNMENT_ATTACHMENT, 다중) +
 * 과목별 제출/채점/수강생 집계(PLM-004 채점 현황과 동일 산식 재사용).
 * 만점은 100 고정(스키마 미보유 — 2026-06-12 확정), 상태는 LEC_ASN_VAL_STATUS 공통코드.
 * (MyBatis resultType은 com.univus.app.lms.dto.LmsAssignmentDto$Inner 형태로 참조)
 */
public final class LmsAssignmentDto {

    private LmsAssignmentDto() {
    }

    /** 등록 폼 과목(강의) 드롭다운 1행 — 교수 담당 강의 (PLM-005 Lecture와 동일 구조) */
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

    /** 과제 1건 — 목록·등록/수정 응답 공용. 제출/채점 집계 + 첨부 배열 포함.
     *  MyBatis가 6+집계 필드를 채우고 service가 ungradedCount 계산·attachments 보강 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Assignment {
        private Long assignmentId;     // LECTURE_ASSIGNMENT.LEC_ASN_ID
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer year;
        private String termCode;
        private String title;
        private String description;    // LEC_ASN_CONTENT (Tiptap HTML, CLOB) — sanitize는 표시단 FE
        private String dueDate;        // "YYYY-MM-DDTHH:mm" (datetime-local 호환, TO_CHAR)
        private String valStatus;      // LEC_ASN_VAL_STATUS (AVL/MOD/LAT/CLS/NOP) — 라벨은 FE 공통코드
        private int submittedCount;    // 제출 학생 수 (LEC_ASN_SBM_STATUS != 'NSB')
        private int gradedCount;       // 채점완료 수 (평가 점수 not null) — FE 미사용, ungraded 계산용
        private int ungradedCount;     // max(0, submitted - graded) — service 계산
        private int totalStudents;     // 수강생 수 (LEC_STD_ENR_STATUS != 'DRP')
        private List<Attachment> attachments; // 유효(ACT) 첨부 전체 (등록순, 없으면 빈 배열)
    }

    /** 첨부 1건 (응답용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attachment {
        private Long attachmentId; // LEC_ASN_ATT_ID — 수정 시 개별 제거 식별자
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 첨부 목록 조회 row (assignmentId 포함 — 서비스에서 과제별 그룹핑용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentListRow {
        private Long assignmentId;
        private Long attachmentId;
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 등록 요청 (multipart/form-data, @ModelAttribute) — 과목·제목·마감일 필수, 설명·첨부 선택 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "대상 과목을 선택해 주세요.")
        private Long lecId;

        @NotBlank(message = "과제명을 입력해 주세요.")
        @Size(max = 200, message = "과제명은 200자 이내여야 합니다.") // DB LEC_ASN_TITLE VARCHAR2(200)
        private String title;

        @NotBlank(message = "마감 일시를 입력해 주세요.")
        private String dueDate; // "YYYY-MM-DDTHH:mm"

        @Size(max = 20000, message = "과제 설명이 너무 깁니다.") // FE 텍스트 4000자, HTML은 더 길어 넉넉한 상한만(CLOB)
        private String description;

        private List<MultipartFile> files; // 선택·다중 — 있을 때만 검증·저장
    }

    /** 수정 요청 — 과목(lecId) 변경 불가(FE 수정 모달 disabled).
     *  첨부(다중): files = 추가 / removeAttachmentIds = 제거 (PLM-005 정책 동일) */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "과제명을 입력해 주세요.")
        @Size(max = 200, message = "과제명은 200자 이내여야 합니다.")
        private String title;

        @NotBlank(message = "마감 일시를 입력해 주세요.")
        private String dueDate;

        @Size(max = 20000, message = "과제 설명이 너무 깁니다.")
        private String description;

        private List<MultipartFile> files;            // 추가 첨부 (교체 아님)
        private List<Long> removeAttachmentIds;       // 제거할 기존 첨부 ID
    }

    /** LECTURE_ASSIGNMENT INSERT 파라미터 — assignmentId는 selectKey(시퀀스)로 채번 후 채워짐 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InsertParam {
        private Long assignmentId;
        private Long lecId;
        private String title;
        private String dueDate;     // "YYYY-MM-DDTHH:mm" → 매퍼에서 TO_TIMESTAMP
        private String description;
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
