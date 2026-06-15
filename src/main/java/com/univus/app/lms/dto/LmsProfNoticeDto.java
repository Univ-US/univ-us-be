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
 * PLM-007 교수 공지사항 관리 DTO 묶음 (3분류 네스티드 — CLAUDE-BE §9 #12).
 * <p>매핑 row(LectureRow/NoticeRow/AttachmentRow/AttachmentDiskRow/InsertParam)
 * + 응답(LectureResDto/NoticeResDto/AttachmentResDto) + 요청(CreateReqDto/UpdateReqDto).
 * 본체=LECTURE_ANNOUNCEMENT, 첨부=LECTURE_ANNOUNCEMENT_ATTACHMENT. PLM-005 업로드 BE 미러
 * (단 목록 페이지네이션·메타 없음 — 선택 강의의 공지 전체를 최신순으로 반환). MyBatis resultType = {@code ...LmsProfNoticeDto$Inner}.
 */
public final class LmsProfNoticeDto {

    private LmsProfNoticeDto() {
    }

    /* ===== 매핑(정본) row — MyBatis resultType/parameterType, 내부 전용 ===== */

    /** 공지 작성 대상 = 담당 강의 드롭다운 1행 (selectLecturesByProfessor) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LectureRow {
        private Long lecId;
        private String courseName;  // LECTURE_CODE.LEC_COD_NAME
        private Integer lecSection; // LECTURE.LEC_SECTION
        private Integer year;       // SEMESTERS.SEM_YEAR
        private String termCode;    // SEMESTERS.SEM_TERM
    }

    /** 공지 본체 1건 매핑 (selectNoticesByLecture / selectNoticeById) — 첨부는 service가 별도 조립 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeRow {
        private Long noticeId;     // LECTURE_ANNOUNCEMENT.LEC_ANN_ID
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer year;
        private String termCode;
        private String title;
        private String content;    // Tiptap HTML(CLOB) — sanitize는 표시단(FE)
        private String author;     // 작성 교수 MEMBER_NAME
        private String regDate;    // "YYYY-MM-DD HH:mm" (LEC_ANN_REG_DATE) — 'date'는 Oracle 예약어라 alias 회피
        private String listDate;   // "MM.DD"
    }

    /** 유효(ACT) 첨부 1행 매핑 (noticeId 포함, 서비스 그룹핑용) */
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

    /** 삭제 시 디스크 파일 정리용 첨부 1행 (selectAttachmentsByNoticeId) — 내부 전용 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDiskRow {
        private Long attachmentId;
        private String trnFileName; // 서버 저장 파일명
    }

    /** 다운로드용 첨부 1행 (소유권+ACT 검증된 1건, selectOwnedActiveAttachment) — 내부 전용 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDownloadRow {
        private String trnFileName; // 서버 저장 파일명(디스크)
        private String fileName;    // 원본 파일명(Content-Disposition)
    }

    /** LECTURE_ANNOUNCEMENT INSERT 파라미터 — noticeId는 selectKey(시퀀스) 채번 후 채워짐 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InsertParam {
        private Long noticeId;
        private Long lecId;
        private String title;
        private String content;
    }

    /* ===== 응답(ResDto) — JSON 프로퍼티명 = FE 계약(불변) ===== */

    /** 공지 작성 대상 강의 드롭다운 응답 */
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
    }

    /** 공지 1건 응답 — 목록·작성/수정 공용. 첨부 ACT 전체 배열(없으면 빈 배열) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeResDto {
        private Long noticeId;
        private Long lecId;
        private String courseName;
        private Integer lecSection;
        private Integer year;
        private String termCode;
        private String title;
        private String content;
        private String author;
        private String date;     // 등록일시 "YYYY-MM-DD HH:mm"
        private String listDate; // 좌측 목록 날짜 "MM.DD"
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
        private Long fileSize;
    }

    /* ===== 요청(ReqDto) ===== */

    /** 작성 요청 (multipart/form-data, @ModelAttribute) — 첨부 선택·다중 */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateReqDto {
        @NotNull(message = "대상 강의를 선택해 주세요.")
        private Long lecId;

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이내여야 합니다.") // DB LEC_ANN_TITLE VARCHAR2(200 CHAR)
        private String title;

        // FE 에디터 기준 텍스트 4000자 — HTML(서식 포함)은 더 길어 넉넉한 상한만 가드(CLOB)
        @Size(max = 20000, message = "공지 내용이 너무 깁니다.")
        private String content;

        private List<MultipartFile> files; // 선택·다중 — 있을 때만 검증·저장
    }

    /** 수정 요청 — 강의(lecId) 변경 불가. files=추가할 새 파일 / removeAttachmentIds=제거할 기존 첨부 ID */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateReqDto {
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
        private String title;

        @Size(max = 20000, message = "공지 내용이 너무 깁니다.")
        private String content;

        private List<MultipartFile> files;      // 추가 첨부 (교체 아님 — 기존에 더해짐)
        private List<Long> removeAttachmentIds; // 제거할 기존 첨부 ID
    }
}
