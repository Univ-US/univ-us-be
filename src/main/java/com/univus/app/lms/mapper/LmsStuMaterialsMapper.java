package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuMaterialsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** SLM-006 학생 강의 자료 Mapper (수강 강의 LECTURE_STUDENT_ENROLLMENT 기준) */
@Mapper
public interface LmsStuMaterialsMapper {

    /** 학생 본인 LMS_PRF_ID 조회 (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 수강 과목(강의) 드롭다운 — 본인 수강(DRP 제외·미시작 학기 제외) 강의 전체 */
    List<LmsStuMaterialsDto.LectureRow> selectEnrolledLectures(@Param("lmsPrfId") Long lmsPrfId);

    /** 선택 과목 자료 개수 (소유권: 본인 수강 강의만. 임의 lecId면 0) */
    int countMaterials(@Param("lmsPrfId") Long lmsPrfId, @Param("lecId") Long lecId);

    /** 선택 과목 자료 1페이지 (소유권 + OFFSET/FETCH. 본체만, 첨부는 별도 조회) */
    List<LmsStuMaterialsDto.MaterialRow> selectMaterialsPaged(@Param("lmsPrfId") Long lmsPrfId,
                                                              @Param("lecId") Long lecId,
                                                              @Param("offset") int offset,
                                                              @Param("size") int size);

    /** 현재 페이지 자료들의 유효(ACT) 첨부 (서비스에서 uploadId 그룹핑) */
    List<LmsStuMaterialsDto.AttachmentRow> selectActiveAttachmentsByUploadIds(
            @Param("uploadIds") List<Long> uploadIds);

    /** 첨부 다운로드 (소유권: 본인 수강 강의 첨부만) */
    LmsStuMaterialsDto.AttachmentDownloadRow selectDownloadAttachment(
            @Param("attachmentId") Long attachmentId,
            @Param("lmsPrfId") Long lmsPrfId);
}
