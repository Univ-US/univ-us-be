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
 * PLM-006 교수 과제 관리 DTO 묶음 (3분류 네스티드).
 * <p>매핑 row(LectureRow/AssignmentRow/AttachmentRow/AttachmentDiskRow) + InsertParam(매퍼 쓰기 파라미터)
 * + 응답(LectureResDto/AssignmentResDto/AttachmentResDto) + 요청(CreateReqDto/UpdateReqDto).
 * <p>과제(LECTURE_ASSIGNMENT) CRUD + 첨부(LECTURE_ASSIGNMENT_ATTACHMENT, 다중) +
 * 과목별 제출/채점/수강생 집계. 만점은 100 고정,
 * 상태는 LEC_ASN_VAL_STATUS 공통코드. service가 매핑 row → ResDto 변환(ungradedCount 계산·첨부 조립).
 * MyBatis resultType은 {@code ...LmsProfAssignmentDto$Inner} 형태로 참조.
 * <p>※ 명명 규칙: BE 정본 + DTO 변수명 = DB 컬럼명 카멜(자기/앵커 LECTURE_ASSIGNMENT 테이블), 조인/파생 컬럼은 의미 별칭 — 커뮤니티 PostDto 컨벤션.
 *    응답 JSON·요청 form key = 컬럼 카멜(lecAsnTitle·lecAsnContent·lecAsnDueDate·lecAsnValStatus·semYear·semTerm).
 *    PK/ID(assignmentId·attachmentId)·파일 메타(fileName·fileExt·fileSize)·집계(submittedCount 등)는 의미명 유지,
 *    InsertParam·매퍼 @Param(title·content·dueDate)은 내부 쓰기 플러밍이라 유지.
 */
public final class LmsProfAssignmentDto {

    private LmsProfAssignmentDto() {
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
        private String courseName;   // LECTURE_CODE.LEC_COD_NAME (조인 → 의미별칭)
        private Integer lecSection;  // LECTURE.LEC_SECTION (조인, 분반)
        private Integer semYear;     // SEMESTERS.SEM_YEAR (조인)
        private String semTerm;      // SEMESTERS.SEM_TERM (조인, 공통코드 SEM_TERM)
        private String lecValStatus; // LECTURE.LEC_VAL_STATUS (조인, 공통코드 LEC_VAL_STATUS)
    }

    /** 과제 본체 1건 매핑 (selectAssignmentsByCoursePaged / selectAssignmentById) — 본체+집계만.
     *  ungradedCount는 service가 max(0, submitted-graded) 계산, 첨부는 service가 별도 조립 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentRow {
        private Long assignmentId;     // LECTURE_ASSIGNMENT.LEC_ASN_ID (자기 PK — 의미명 유지)
        private Long lecId;
        private String courseName;     // LECTURE_CODE.LEC_COD_NAME (조인)
        private Integer lecSection;    // LECTURE.LEC_SECTION (조인, 분반)
        private Integer semYear;       // SEMESTERS.SEM_YEAR (조인)
        private String semTerm;        // SEMESTERS.SEM_TERM (조인)
        private String lecAsnTitle;    // LECTURE_ASSIGNMENT.LEC_ASN_TITLE (자기 컬럼)
        private String lecAsnContent;  // LECTURE_ASSIGNMENT.LEC_ASN_CONTENT (자기 컬럼, Tiptap HTML CLOB — sanitize는 표시단 FE)
        private String lecAsnDueDate;  // LECTURE_ASSIGNMENT.LEC_ASN_DUE_DATE "YYYY-MM-DDTHH:mm" (자기 컬럼, datetime-local 호환 TO_CHAR)
        private String lecAsnValStatus;// LECTURE_ASSIGNMENT.LEC_ASN_VAL_STATUS (자기 컬럼, AVL/MOD/LAT/CLS/NOP)
        private int submittedCount;    // 제출 학생 수 (LEC_ASN_SBM_STATUS != 'NSB') — 집계
        private int gradedCount;       // 채점완료 수 (평가 점수 not null) — ungraded 계산용
        private int totalStudents;     // 수강생 수 (LEC_STD_ENR_STATUS != 'DRP') — 집계
    }

    /** 유효(ACT) 첨부 1행 매핑 (selectActiveAttachmentsByAssignmentId... — assignmentId 포함, 서비스 그룹핑용) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentRow {
        private Long assignmentId;
        private Long attachmentId;
        private String fileName;
        private String fileExt;
        private Long fileSize;
    }

    /** 삭제 시 디스크 파일 정리용 첨부 1행 매핑 (selectAttachmentsByAssignmentId) — 내부 전용 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDiskRow {
        private Long attachmentId;
        private String trnFileName; // 서버 저장 파일명
    }

    /** LECTURE_ASSIGNMENT INSERT 파라미터 — assignmentId는 selectKey(시퀀스)로 채번 후 채워짐 (FE 무관 매퍼 입출력) */
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
        private Integer semYear;     // SEMESTERS.SEM_YEAR
        private String semTerm;      // SEMESTERS.SEM_TERM
        private String lecValStatus;
    }

    /** 과제 1건 응답 — 목록·등록/수정 공용. 제출/채점 집계 + 첨부 ACT 전체 배열(없으면 빈 배열) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignmentResDto {
        private Long assignmentId;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer semYear;       // SEMESTERS.SEM_YEAR
        private String semTerm;        // SEMESTERS.SEM_TERM
        private String lecAsnTitle;    // LECTURE_ASSIGNMENT.LEC_ASN_TITLE
        private String lecAsnContent;  // LECTURE_ASSIGNMENT.LEC_ASN_CONTENT
        private String lecAsnDueDate;  // LECTURE_ASSIGNMENT.LEC_ASN_DUE_DATE "YYYY-MM-DDTHH:mm"
        private String lecAsnValStatus;// LECTURE_ASSIGNMENT.LEC_ASN_VAL_STATUS (라벨은 FE 공통코드)
        private int submittedCount;
        private int gradedCount;       // FE 미사용, ungraded 계산 산식 노출
        private int ungradedCount;     // max(0, submitted - graded) — service 계산
        private int totalStudents;
        private List<AttachmentResDto> attachments; // 유효(ACT) 첨부 전체 (등록순, 없으면 빈 배열)
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

    /* ===== 요청(ReqDto) ===== */

    /** 등록 요청 (multipart/form-data, @ModelAttribute) — 과목·제목·마감일 필수, 설명·첨부 선택 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateReqDto {
        @NotNull(message = "대상 과목을 선택해 주세요.")
        private Long lecId;

        @NotBlank(message = "과제명을 입력해 주세요.")
        @Size(max = 200, message = "과제명은 200자 이내여야 합니다.") // DB LEC_ASN_TITLE VARCHAR2(200)
        private String lecAsnTitle;   // LECTURE_ASSIGNMENT.LEC_ASN_TITLE (멀티파트 form key)

        @NotBlank(message = "마감 일시를 입력해 주세요.")
        private String lecAsnDueDate; // LECTURE_ASSIGNMENT.LEC_ASN_DUE_DATE "YYYY-MM-DDTHH:mm"

        @Size(max = 20000, message = "과제 설명이 너무 깁니다.") // FE 텍스트 4000자, HTML은 더 길어 넉넉한 상한만(CLOB)
        private String lecAsnContent; // LECTURE_ASSIGNMENT.LEC_ASN_CONTENT (에디터 HTML)

        private List<MultipartFile> files; // 선택·다중 — 있을 때만 검증·저장
    }

    /** 수정 요청 — 과목(lecId) 변경 불가(FE 수정 모달 disabled).
     *  첨부(다중): files = 추가 / removeAttachmentIds = 제거 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateReqDto {
        @NotBlank(message = "과제명을 입력해 주세요.")
        @Size(max = 200, message = "과제명은 200자 이내여야 합니다.")
        private String lecAsnTitle;   // LECTURE_ASSIGNMENT.LEC_ASN_TITLE

        @NotBlank(message = "마감 일시를 입력해 주세요.")
        private String lecAsnDueDate; // LECTURE_ASSIGNMENT.LEC_ASN_DUE_DATE

        @Size(max = 20000, message = "과제 설명이 너무 깁니다.")
        private String lecAsnContent; // LECTURE_ASSIGNMENT.LEC_ASN_CONTENT

        private List<MultipartFile> files;            // 추가 첨부 (교체 아님)
        private List<Long> removeAttachmentIds;       // 제거할 기존 첨부 ID
    }
}
