package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfNoticeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** PLM-007 교수 공지사항 Mapper (LECTURE_ANNOUNCEMENT / LECTURE_ANNOUNCEMENT_ATTACHMENT) */
@Mapper
public interface LmsProfNoticeMapper {

    /** 교수 본인 LMS_PRF_ID 조회 (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 공지 작성 대상 = 교수 담당 강의 전체 */
    List<LmsProfNoticeDto.LectureRow> selectLecturesByProfessor(@Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 본인 담당 강의 여부 (작성 시 소유권 검증) */
    int countOwnedLecture(@Param("lecId") Long lecId,
                          @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 본인 강의의 공지 여부 (수정/삭제 시 소유권 검증) */
    int countOwnedNotice(@Param("noticeId") Long noticeId,
                         @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 선택 강의의 공지 개수 (소유권 스코프 — 타 교수 강의면 0) */
    long countNotices(@Param("lecId") Long lecId,
                      @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 선택 강의의 공지 1페이지 (최신순 + OFFSET/FETCH) — 본인 강의로 스코프. 첨부는 별도 조회 */
    List<LmsProfNoticeDto.NoticeRow> selectNoticesByLecturePaged(@Param("lecId") Long lecId,
                                                                 @Param("professorLmsPrfId") Long professorLmsPrfId,
                                                                 @Param("offset") int offset,
                                                                 @Param("size") int size);

    /** 공지 단건 (작성/수정 응답용 — 본체만) */
    LmsProfNoticeDto.NoticeRow selectNoticeById(@Param("noticeId") Long noticeId);

    /** 주어진 공지들의 유효(ACT) 첨부 목록 (서비스에서 noticeId로 그룹핑) */
    List<LmsProfNoticeDto.AttachmentRow> selectActiveAttachmentsByNoticeIds(@Param("noticeIds") List<Long> noticeIds);

    /** 공지 1건의 유효(ACT) 첨부 목록 */
    List<LmsProfNoticeDto.AttachmentRow> selectActiveAttachmentsByNoticeId(@Param("noticeId") Long noticeId);

    /** 다운로드용: 소유권(본인 강의)+유효(ACT) 검증된 첨부 1건 (없으면 null → 404) */
    LmsProfNoticeDto.AttachmentDownloadRow selectOwnedActiveAttachment(@Param("attachmentId") Long attachmentId,
                                                                       @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 공지 본체 INSERT — noticeId는 selectKey(SEQ_LECTURE_ANNOUNCEMENT)로 채번되어 param에 채워짐 */
    int insertNotice(LmsProfNoticeDto.InsertParam param);

    /** 첨부 INSERT (ATT_VAL_STATUS='ACT') — 다중 첨부 허용(공지당 N건) */
    int insertAttachment(@Param("noticeId") Long noticeId,
                         @Param("orgFileName") String orgFileName,
                         @Param("trnFileName") String trnFileName,
                         @Param("orgUrl") String orgUrl,
                         @Param("fileSize") Long fileSize,
                         @Param("extType") String extType);

    /** 공지 본체 수정 (제목·내용) */
    int updateNotice(@Param("noticeId") Long noticeId,
                     @Param("title") String title,
                     @Param("content") String content);

    /** 기존 첨부 1건 무효화 (ATT_VAL_STATUS 'DEL' — noticeId 조건으로 소유권 체인 보장) */
    int invalidateAttachmentById(@Param("noticeId") Long noticeId,
                                 @Param("attachmentId") Long attachmentId);

    /** 공지의 첨부 전체 조회 (삭제 시 디스크 파일 정리용) */
    List<LmsProfNoticeDto.AttachmentDiskRow> selectAttachmentsByNoticeId(@Param("noticeId") Long noticeId);

    /** 첨부 물리 삭제 (본체 삭제 전 FK 선행) */
    int deleteAttachments(@Param("noticeId") Long noticeId);

    /** 공지 본체 물리 삭제 */
    int deleteNotice(@Param("noticeId") Long noticeId);
}
