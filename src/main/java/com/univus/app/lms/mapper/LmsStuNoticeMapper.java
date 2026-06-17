package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuNoticeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuNoticeMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    List<LmsStuNoticeDto.NoticeRow> selectStudentNotices(@Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuNoticeDto.AttachmentRow> selectActiveAttachmentsByNoticeIds(@Param("noticeIds") List<Long> noticeIds);

    LmsStuNoticeDto.AttachmentDownloadRow selectOwnedActiveAttachment(
            @Param("attachmentId") Long attachmentId,
            @Param("lmsPrfId") Long lmsPrfId);
}
