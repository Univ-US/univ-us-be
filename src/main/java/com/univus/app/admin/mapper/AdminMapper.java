package com.univus.app.admin.mapper;

import com.univus.app.admin.dto.AdminDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

    List<AdminDto.MemberListDto> selectMemberList(AdminDto.MemberSearchDto search);

    int countMemberList(AdminDto.MemberSearchDto search);

    int updateMemberStatus(AdminDto.MemberStatusDto memberStatusDto);

    int insertNotice(AdminDto.NoticeDto notice);

    int updateNotice(AdminDto.NoticeDto notice);

    int deleteNotice(@Param("noticeId") Long noticeId);

    List<AdminDto.NoticeListDto> selectNoticeList(@Param("univId") Long univId, @Param("role") String role);

    int insertSupport(AdminDto.SupportRequestDto support);

    List<AdminDto.SupportListDto> selectSupportList(@Param("univId") Long univId);

    int updateSupportStatus(AdminDto.SupportStatusDto dto);

    List<AdminDto.UniversityDto> selectUniversityList();

    AdminDto.UniversityDto selectUniversityById(@Param("univId") Long univId);

    int updateUniversity(@Param("univId") Long univId, @Param("dto") AdminDto.UniversityUpdateDto dto);

    List<AdminDto.DepartmentDto> selectDepartmentList(@Param("univId") Long univId);

    List<AdminDto.LectureCodeListDto> selectLectureCodeList(@Param("univId") Long univId);

    int insertLectureCode(AdminDto.LectureCodeDto dto);

    int updateLectureCode(AdminDto.LectureCodeDto dto);

    int updateLectureCodeStatus(AdminDto.LectureCodeStatusDto dto);

    int deleteLectureCode(@Param("lectureCodeId") Long lectureCodeId);

    AdminDto.HomeConfigDto selectHomeConfig(@Param("univId") Long univId);

    int insertHomeConfigIfNotExists(@Param("univId") Long univId);

    int updateHomeConfig(@Param("univId") Long univId, @Param("dto") AdminDto.HomeConfigDto dto);

    AdminDto.NoticeConfigDto selectNoticeConfig(@Param("univId") Long univId);

    int insertNoticeConfigIfNotExists(@Param("univId") Long univId);

    int updateNoticeConfig(@Param("univId") Long univId, @Param("dto") AdminDto.NoticeConfigDto dto);

    List<AdminDto.LectureAssignListDto> selectLectureAssignList(@Param("univId") Long univId, @Param("semId") Long semId);

    AdminDto.LectureAssignListDto selectLectureAssignById(@Param("lecId") Long lecId);

    int insertLectureAssign(AdminDto.LectureAssignCreateDto dto);

    int insertLectureTime(@Param("lecId") Long lecId, @Param("time") AdminDto.LectureTimeDto time);

    int selectNextLectureSection(@Param("semId") Long semId, @Param("lecCodeId") Long lecCodeId);

    Long selectLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    int countProfessorTimeConflicts(@Param("lmsPrfId") Long lmsPrfId, @Param("semId") Long semId,
                                    @Param("dayCode") String dayCode,
                                    @Param("startTime") String startTime, @Param("endTime") String endTime,
                                    @Param("excludeLecId") Long excludeLecId);

    int updateLectureAssign(AdminDto.LectureAssignCreateDto dto);

    int deleteLectureTimes(@Param("lecId") Long lecId);

    int insertLmsProfile(@Param("memberId") Long memberId);

    List<AdminDto.SemesterDto> selectSemesterList();

    List<AdminDto.ProfessorDto> selectProfessorList(@Param("univId") Long univId);

    List<AdminDto.LectureListDto> selectLectureList(@Param("univId") Long univId);

    int countLectureAssigns(@Param("lecCodeId") Long lecCodeId);

    int countLectureCodeDup(@Param("deptId") Long deptId, @Param("lecCode") String lecCode,
                            @Param("excludeLecCodeId") Long excludeLecCodeId);

    int softDeleteLecture(@Param("lecCodeId") Long lecCodeId);

    int updateLectureAssignStatusBySemester(@Param("univId") Long univId, @Param("semId") Long semId,
                                             @Param("fromStatus") String fromStatus, @Param("newStatus") String newStatus);

    /** 대상 학기를 수강신청중(OPEN)으로 — 예정(SCHD)/마감(PROG) 강좌만 전환 */
    int openLecturesForSemester(@Param("univId") Long univId, @Param("semId") Long semId);

    /** 대상 학기보다 시작일이 이른 학기의 강좌 전체 종료(CLSD) — 폐강(CNCL)은 보존 */
    int closeLecturesBeforeSemester(@Param("univId") Long univId, @Param("semId") Long semId);

    /** 대상 학기보다 시작일이 늦은 학기의 강좌 전체 예정(SCHD)으로 — 폐강(CNCL)은 보존 */
    int scheduleLecturesAfterSemester(@Param("univId") Long univId, @Param("semId") Long semId);
}
