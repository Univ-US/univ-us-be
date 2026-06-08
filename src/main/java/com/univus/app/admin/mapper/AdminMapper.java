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

    List<AdminDto.NoticeListDto> selectNoticeList();

    int insertSupport(AdminDto.SupportRequestDto support);

    List<AdminDto.SupportListDto> selectSupportList(@Param("univId") Long univId);

    List<AdminDto.UniversityDto> selectUniversityList();
}
