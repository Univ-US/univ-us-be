package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuMaterialsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuMaterialsMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    List<LmsStuMaterialsDto.MaterialFlatRow> selectMaterialRows(@Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuMaterialsDto.AttachmentRow> selectActiveAttachmentsByUploadIds(
            @Param("uploadIds") List<Long> uploadIds);

    LmsStuMaterialsDto.AttachmentDownloadRow selectDownloadAttachment(
            @Param("attachmentId") Long attachmentId,
            @Param("lmsPrfId") Long lmsPrfId);
}
