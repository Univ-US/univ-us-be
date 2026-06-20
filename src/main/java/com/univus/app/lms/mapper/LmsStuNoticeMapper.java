package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuNoticeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SLM-009 학생 공지 Mapper (수강 강의 LECTURE_STUDENT_ENROLLMENT 기준) */
@Mapper
public interface LmsStuNoticeMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 수강 과목(강의) 드롭다운 — 본인 수강(DRP 제외·미시작 학기 제외) 강의 전체 */
    List<LmsStuNoticeDto.LectureRow> selectEnrolledLectures(@Param("lmsPrfId") Long lmsPrfId);

    /** 선택 과목 공지 개수 (소유권: 본인 수강 강의만. 임의 lecId면 0) */
    int countNotices(@Param("lmsPrfId") Long lmsPrfId, @Param("lecId") Long lecId);

    /** 선택 과목 공지 1페이지 (소유권 + OFFSET/FETCH. 본체만, 첨부는 별도 조회) */
    List<LmsStuNoticeDto.NoticeRow> selectNoticesPaged(@Param("lmsPrfId") Long lmsPrfId,
                                                       @Param("lecId") Long lecId,
                                                       @Param("offset") int offset,
                                                       @Param("size") int size);

    /** 현재 페이지 공지들의 유효(ACT) 첨부 (서비스에서 noticeId 그룹핑) */
    List<LmsStuNoticeDto.AttachmentRow> selectActiveAttachmentsByNoticeIds(@Param("noticeIds") List<Long> noticeIds);

    /** 첨부 다운로드 (소유권: 본인 수강 강의 첨부만) */
    LmsStuNoticeDto.AttachmentDownloadRow selectOwnedActiveAttachment(
            @Param("attachmentId") Long attachmentId,
            @Param("lmsPrfId") Long lmsPrfId);
}
