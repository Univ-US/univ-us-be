package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsProfEnrolleeDto;
import com.univus.app.lms.dto.LmsProfLectureDto;
import com.univus.app.lms.dto.LmsProfSemesterDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsProfEnrolleeMapper {

    /* 교수 본인 LMS_PRF_ID (없으면 null) */
    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /* 학기 목록 (드롭다운) */
    List<LmsProfSemesterDto> selectSemesters();

    /* 교수 담당 강의 목록 (드롭다운, semId optional) */
    List<LmsProfLectureDto> selectLecturesByProfessor(@Param("lmsPrfId") Long lmsPrfId,
                                                  @Param("semId") Long semId);

    /* 강의 헤더(+소유권 검증). 본인 강의 아니면 null */
    LmsProfLectureDto selectLectureHeader(@Param("lecId") Long lecId,
                                      @Param("professorLmsPrfId") Long professorLmsPrfId);

    /* 강의의 유효 과제 수 (과제 제출 분모) */
    int countAssignments(@Param("lecId") Long lecId);

    /* PLM-003 수강생 목록 (출석/제출/평균 raw 집계 포함) */
    List<LmsProfEnrolleeDto.Row> selectStudentRows(@Param("lecId") Long lecId,
                                             @Param("professorLmsPrfId") Long professorLmsPrfId,
                                             @Param("search") String search);

    /* PLM-003-01 상세 리포트 헤더(학생 정보 + 출결/제출/평균 집계). 본인 강의·수강생 아니면 null */
    LmsProfEnrolleeDto.ReportRow selectStudentReportHeader(@Param("lecId") Long lecId,
                                                  @Param("professorLmsPrfId") Long professorLmsPrfId,
                                                  @Param("memberId") Long memberId);

    /* PLM-003-01 과제별 점수 */
    List<LmsProfEnrolleeDto.AssignmentScoreRow> selectAssignmentScores(@Param("lecId") Long lecId,
                                                       @Param("studentLmsPrfId") Long studentLmsPrfId);
}
