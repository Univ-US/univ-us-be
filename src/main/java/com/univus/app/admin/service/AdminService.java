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

    public Map<String, Object> getMemberList(AdminDto.MemberSearchDto search, Long requesterId) {
        if (search.getSize() <= 0) search.setSize(10);
        if (search.getPage() <= 0) search.setPage(1);

        MemberDto requester = memberMapper.findByMemberId(requesterId);
        if ("ADM".equals(requester.getRole())) {
            search.setUnivId(requester.getUnivId());
        }

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
    public void createNotice(AdminDto.NoticeDto notice, Long requesterId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        notice.setMemberId(requesterId);
        notice.setUnivId(requester.getUnivId());
        adminMapper.insertNotice(notice);
    }

    @Transactional
    public void updateNotice(AdminDto.NoticeDto notice) {
        adminMapper.updateNotice(notice);
    }

    public void deleteNotice(Long noticeId) {
        adminMapper.deleteNotice(noticeId);
    }

    public List<AdminDto.NoticeListDto> getNoticeList(Long requesterId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        String role = requester.getRole();
        String filterRole = "ADM".equals(role) ? null : role;
        return adminMapper.selectNoticeList(requester.getUnivId(), filterRole);
    }

    public void createSupport(AdminDto.SupportRequestDto support) {
        adminMapper.insertSupport(support);
    }

    public List<AdminDto.SupportListDto> getSupportList(Long memberId, Long univId) {
        MemberDto member = memberMapper.findByMemberId(memberId);
        Long effectiveUnivId = "SUA".equals(member.getRole()) ? univId : member.getUnivId();
        return adminMapper.selectSupportList(effectiveUnivId);
    }

    public void updateSupportStatus(Long supportId, Integer status) {
        AdminDto.SupportStatusDto dto = new AdminDto.SupportStatusDto();
        dto.setSupportId(supportId);
        dto.setStatus(status);
        adminMapper.updateSupportStatus(dto);
    }

    public List<AdminDto.UniversityDto> getUniversityList() {
        return adminMapper.selectUniversityList();
    }

    public AdminDto.UniversityDto getUniversity(Long univId) {
        return adminMapper.selectUniversityById(univId);
    }

    @Transactional
    public void updateUniversity(Long univId, AdminDto.UniversityUpdateDto dto) {
        adminMapper.updateUniversity(univId, dto);
    }

    public List<AdminDto.DepartmentDto> getDepartmentList(Long requesterId, Long univId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        Long effectiveUnivId = "SUA".equals(requester.getRole()) ? univId : requester.getUnivId();
        return adminMapper.selectDepartmentList(effectiveUnivId);
    }

    public List<AdminDto.LectureCodeListDto> getLectureCodeList(Long requesterId, Long univId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        Long effectiveUnivId = "SUA".equals(requester.getRole()) ? univId : requester.getUnivId();
        return adminMapper.selectLectureCodeList(effectiveUnivId);
    }

    @Transactional
    public void createLectureCode(AdminDto.LectureCodeDto dto) {
        adminMapper.insertLectureCode(dto);
    }

    @Transactional
    public void updateLectureCode(AdminDto.LectureCodeDto dto) {
        adminMapper.updateLectureCode(dto);
    }

    @Transactional
    public void updateLectureCodeStatus(Long lecCodeId, String valStatus) {
        AdminDto.LectureCodeStatusDto dto = new AdminDto.LectureCodeStatusDto();
        dto.setLecCodeId(lecCodeId);
        dto.setValStatus(valStatus);
        adminMapper.updateLectureCodeStatus(dto);
    }

    @Transactional
    public void deleteLectureCode(Long lectureCodeId) {
        adminMapper.deleteLectureCode(lectureCodeId);
    }
}
