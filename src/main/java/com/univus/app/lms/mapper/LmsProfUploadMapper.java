package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfUploadDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** PLM-005 / PLM-005-01 교수 강의 자료 업로드 Mapper (LECTURE_UPLOADING / LECTURE_UPLOADING_ATTACHMENT) */
@Mapper
public interface LmsProfUploadMapper {

    /** 교수 본인 LMS_PRF_ID 조회 (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    List<LmsProfUploadDto.LectureRow> selectLecturesByProfessor(@Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 본인 담당 강의 여부 (등록 시 소유권 검증) */
    int countOwnedLecture(@Param("lecId") Long lecId,
                          @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 본인이 등록한(=본인 강의의) 자료 여부 (수정/삭제 시 소유권 검증) */
    int countOwnedUpload(@Param("uploadId") Long uploadId,
                         @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 자료 개수 (서버 페이지네이션 — 년도/학기 필터 선택 적용. year/termCode null이면 전체) */
    int countUploadsByProfessor(@Param("professorLmsPrfId") Long professorLmsPrfId,
                                @Param("year") Integer year,
                                @Param("termCode") String termCode);

    /** 자료 목록 1페이지 (서버 페이지네이션 — 년도/학기 필터 + OFFSET/FETCH. 본체만, 첨부는 별도 조회) */
    List<LmsProfUploadDto.MaterialRow> selectUploadsPaged(@Param("professorLmsPrfId") Long professorLmsPrfId,
                                                   @Param("year") Integer year,
                                                   @Param("termCode") String termCode,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size);

    /** 목록 필터 옵션 — 자료가 존재하는 년도/학기 distinct (드롭다운 소스) */
    List<LmsProfUploadDto.SemesterOptionRow> selectDistinctMaterialSemesters(
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 자료 단건 (등록/수정 응답용 — 본체만) */
    LmsProfUploadDto.MaterialRow selectUploadById(@Param("uploadId") Long uploadId);

    /** 주어진 자료들(현재 페이지 uploadId들)의 유효(ACT) 첨부 목록 (서비스에서 자료별 그룹핑) */
    List<LmsProfUploadDto.AttachmentRow> selectActiveAttachmentsByUploadIds(
            @Param("uploadIds") List<Long> uploadIds);

    /** 자료 1건의 유효(ACT) 첨부 목록 */
    List<LmsProfUploadDto.AttachmentRow> selectActiveAttachmentsByUploadId(@Param("uploadId") Long uploadId);

    /** 자료 본체 INSERT — uploadId는 selectKey(SEQ_LECTURE_UPLOADING)로 채번되어 param에 채워짐 */
    int insertUpload(LmsProfUploadDto.InsertParam param);

    /** 첨부 INSERT (ATT_VAL_STATUS='ACT') — 다중 첨부 허용(자료당 N건) */
    int insertAttachment(@Param("uploadId") Long uploadId,
                         @Param("orgFileName") String orgFileName,
                         @Param("trnFileName") String trnFileName,
                         @Param("orgUrl") String orgUrl,
                         @Param("fileSize") Long fileSize,
                         @Param("extType") String extType);

    /** 자료 본체 수정 (제목·설명) */
    int updateUpload(@Param("uploadId") Long uploadId,
                     @Param("title") String title,
                     @Param("content") String content);

    /** 기존 첨부 1건 무효화 (공통코드 ATT_VAL_STATUS 'DEL' — uploadId 조건으로 소유권 체인 보장) */
    int invalidateAttachmentById(@Param("uploadId") Long uploadId,
                                 @Param("attachmentId") Long attachmentId);

    /** 자료의 첨부 전체 조회 (삭제 시 디스크 파일 정리용) */
    List<LmsProfUploadDto.AttachmentDiskRow> selectAttachmentsByUploadId(@Param("uploadId") Long uploadId);

    /** 첨부 물리 삭제 (본체 삭제 전 FK 선행) */
    int deleteAttachments(@Param("uploadId") Long uploadId);

    /** 자료 본체 물리 삭제 */
    int deleteUpload(@Param("uploadId") Long uploadId);
}
