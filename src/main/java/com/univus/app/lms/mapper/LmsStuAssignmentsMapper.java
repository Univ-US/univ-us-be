package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuAssignmentsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuAssignmentsMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    List<LmsStuAssignmentsDto.AssignmentFlatRow> selectAssignmentRows(@Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuAssignmentsDto.SubmittableRow> selectSubmittableRows(@Param("lmsPrfId") Long lmsPrfId);

    LmsStuAssignmentsDto.SubmitAccessRow selectSubmitAccess(
            @Param("assignmentId") Long assignmentId,
            @Param("lmsPrfId") Long lmsPrfId);

    LmsStuAssignmentsDto.SubmitAccessRow selectSubmissionAccess(
            @Param("submissionId") Long submissionId,
            @Param("lmsPrfId") Long lmsPrfId);

    List<LmsStuAssignmentsDto.FileRow> selectSubmissionFiles(@Param("submissionId") Long submissionId);

    int insertSubmission(LmsStuAssignmentsDto.SubmitParam param);

    int updateSubmissionAsSubmitted(
            @Param("submissionId") Long submissionId,
            @Param("memo") String memo);

    int updateSubmissionMemo(
            @Param("submissionId") Long submissionId,
            @Param("memo") String memo);

    int updateSubmissionAsNotSubmitted(@Param("submissionId") Long submissionId);

    int insertSubmissionAttachment(
            @Param("submissionId") Long submissionId,
            @Param("orgFileName") String orgFileName,
            @Param("trnFileName") String trnFileName,
            @Param("orgUrl") String orgUrl,
            @Param("fileSize") Long fileSize,
            @Param("extType") String extType);

    int invalidateSubmissionAttachments(@Param("submissionId") Long submissionId);
}
