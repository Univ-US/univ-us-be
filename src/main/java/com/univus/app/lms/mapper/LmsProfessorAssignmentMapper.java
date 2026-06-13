package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsAssignmentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** PLM-006 교수 과제 관리 Mapper (LECTURE_ASSIGNMENT / LECTURE_ASSIGNMENT_ATTACHMENT + 제출/채점/수강생 집계) */
@Mapper
public interface LmsProfessorAssignmentMapper {

    /** 교수 본인 LMS_PRF_ID 조회 (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    List<LmsAssignmentDto.Lecture> selectLecturesByProfessor(@Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 본인 담당 강의 여부 (등록 시 소유권 검증) */
    int countOwnedLecture(@Param("lecId") Long lecId,
                          @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 본인이 등록한(=본인 강의의) 과제 여부 (수정/삭제 시 소유권 검증) */
    int countOwnedAssignment(@Param("assignmentId") Long assignmentId,
                             @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 과제 목록 — 교수 담당 강의 전체 과제 + 제출/채점/수강생 집계 (첨부는 별도 조회 후 그룹핑) */
    List<LmsAssignmentDto.Assignment> selectAssignmentsByProfessor(
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 과제 단건 + 집계 (등록/수정 응답용 — 본체만) */
    LmsAssignmentDto.Assignment selectAssignmentById(@Param("assignmentId") Long assignmentId);

    /** 교수 전체 과제의 유효(ACT) 첨부 목록 (assignmentId 포함 — 서비스에서 과제별 그룹핑) */
    List<LmsAssignmentDto.AttachmentListRow> selectActiveAttachmentsByProfessor(
            @Param("professorLmsPrfId") Long professorLmsPrfId);

    /** 과제 1건의 유효(ACT) 첨부 목록 */
    List<LmsAssignmentDto.AttachmentListRow> selectActiveAttachmentsByAssignmentId(
            @Param("assignmentId") Long assignmentId);

    /** 과제 본체 INSERT — assignmentId는 selectKey(SEQ_LECTURE_ASSIGNMENT)로 채번되어 param에 채워짐 */
    int insertAssignment(LmsAssignmentDto.InsertParam param);

    /** 첨부 INSERT (ATT_VAL_STATUS='ACT') — 다중 첨부 허용(과제당 N건) */
    int insertAttachment(@Param("assignmentId") Long assignmentId,
                         @Param("orgFileName") String orgFileName,
                         @Param("trnFileName") String trnFileName,
                         @Param("orgUrl") String orgUrl,
                         @Param("fileSize") Long fileSize,
                         @Param("extType") String extType);

    /** 과제 본체 수정 (제목·마감일·설명) */
    int updateAssignment(@Param("assignmentId") Long assignmentId,
                         @Param("title") String title,
                         @Param("dueDate") String dueDate,
                         @Param("description") String description);

    /** 기존 첨부 1건 무효화 (공통코드 ATT_VAL_STATUS 'DEL' — assignmentId 조건으로 소유권 체인 보장) */
    int invalidateAttachmentById(@Param("assignmentId") Long assignmentId,
                                 @Param("attachmentId") Long attachmentId);

    /** 과제의 첨부 전체 조회 (삭제 시 디스크 파일 정리용) */
    List<LmsAssignmentDto.AttachmentRow> selectAttachmentsByAssignmentId(@Param("assignmentId") Long assignmentId);

    /** 첨부 물리 삭제 (본체 삭제 전 FK 선행) */
    int deleteAttachments(@Param("assignmentId") Long assignmentId);

    /** 과제 본체 물리 삭제 */
    int deleteAssignment(@Param("assignmentId") Long assignmentId);
}
