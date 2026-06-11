package com.univus.app.admin.mapper;

import com.univus.app.admin.dto.AdminDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

    List<AdminDto.MemberListDto> selectMemberList(AdminDto.MemberSearchDto search);

    int countMemberList(AdminDto.MemberSearchDto search);

    int insertMemberBulk(@Param("members") List<AdminDto.MemberItemDto> members);

    int insertMemberDetailBulk(@Param("members") List<AdminDto.MemberItemDto> members);

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
}
