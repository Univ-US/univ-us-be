package com.univus.app.admin.service;

import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.mapper.AdminMapper;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> getMemberList(AdminDto.MemberSearchDto search) {
        if (search.getSize() <= 0) search.setSize(10);
        if (search.getPage() <= 0) search.setPage(1);

        List<AdminDto.MemberListDto> list = adminMapper.selectMemberList(search);
        int total = adminMapper.countMemberList(search);

        return Map.of("list", list, "total", total);
    }

    @Transactional
    public void registerBulkMembers(List<AdminDto.MemberItemDto> members) {
        members.forEach(m -> m.setPassword(passwordEncoder.encode(m.getPassword())));
        adminMapper.insertMemberBulk(members);
        adminMapper.insertMemberDetailBulk(members);
    }

    public void updateMemberStatus(AdminDto.MemberStatusDto memberStatusDto) {
        adminMapper.updateMemberStatus(memberStatusDto);
    }

    @Transactional
    public void createNotice(AdminDto.NoticeDto notice) {
        adminMapper.insertNotice(notice);
    }

    @Transactional
    public void updateNotice(AdminDto.NoticeDto notice) {
        adminMapper.updateNotice(notice);
    }

    public void deleteNotice(Long noticeId) {
        adminMapper.deleteNotice(noticeId);
    }

    public List<AdminDto.NoticeListDto> getNoticeList() {
        return adminMapper.selectNoticeList();
    }

    public void createSupport(AdminDto.SupportRequestDto support) {
        adminMapper.insertSupport(support);
    }

    public List<AdminDto.SupportListDto> getSupportList(Long memberId) {
        MemberDto member = memberMapper.findByMemberId(memberId);
        Long univId = "SUA".equals(member.getRole()) ? null : member.getUnivId();
        return adminMapper.selectSupportList(univId);
    }

    public List<AdminDto.UniversityDto> getUniversityList() {
        return adminMapper.selectUniversityList();
    }

    public AdminDto.UniversityDto getUniversity(Long univId) {
        return adminMapper.selectUniversityById(univId);
    }
}
