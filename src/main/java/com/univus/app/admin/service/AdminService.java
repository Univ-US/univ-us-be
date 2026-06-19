package com.univus.app.admin.service;

import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.mapper.AdminMapper;
import com.univus.app.ai.service.AsyncEmbeddingService;
import com.univus.app.commoncode.code.RoleCode;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.subscription.service.SubscriptionMemberLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int MAX_BULK_MEMBERS = 500;
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^010\\d{8}$");
    private static final Pattern BIRTH_PATTERN = Pattern.compile("^\\d{8}$");

    private final AdminMapper adminMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final AsyncEmbeddingService asyncEmbeddingService;
    private final SubscriptionMemberLimitService subscriptionMemberLimitService;

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
    public AdminDto.MemberBulkResponseDto registerBulkMembers(
            List<AdminDto.MemberItemDto> members,
            Long requesterId
    ) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        if (requester == null || requester.getUnivId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "회원 등록 대상 대학을 확인할 수 없습니다."
            );
        }
        if (members == null || members.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "등록할 회원이 없습니다."
            );
        }
        if (members.size() > MAX_BULK_MEMBERS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "한 번에 등록할 수 있는 회원은 최대 " + MAX_BULK_MEMBERS + "건입니다."
            );
        }

        subscriptionMemberLimitService.validateAdditionalMembers(
                requester.getUnivId(),
                members.size()
        );

        List<AdminDto.MemberBulkResultDto> results = new ArrayList<>();
        Set<String> registeredLoginIds = new HashSet<>();
        int successCount = 0;

        for (AdminDto.MemberItemDto item : members) {
            AdminDto.MemberBulkResultDto result = new AdminDto.MemberBulkResultDto();
            result.setLoginId(item.getLoginId());
            result.setMemberName(item.getMemberName());

            String error = validateMemberItem(item);
            if (error == null && !registeredLoginIds.add(item.getLoginId())) {
                error = "요청 내에 중복된 로그인ID입니다.";
            }
            if (error == null && memberMapper.existsByLoginId(item.getLoginId()) > 0) {
                error = "이미 사용 중인 로그인ID입니다.";
            }

            if (error != null) {
                result.setSuccess(false);
                result.setMessage(error);
                results.add(result);
                continue;
            }

            try {
                insertBulkMember(item, requester.getUnivId());
                result.setSuccess(true);
                successCount++;
            } catch (Exception e) {
                result.setSuccess(false);
                result.setMessage("회원 등록 중 오류가 발생했습니다.");
            }
            results.add(result);
        }

        AdminDto.MemberBulkResponseDto response = new AdminDto.MemberBulkResponseDto();
        response.setSuccessCount(successCount);
        response.setFailCount(results.size() - successCount);
        response.setResults(results);
        return response;
    }

    private void insertBulkMember(AdminDto.MemberItemDto item, Long univId) {
        MemberDto member = new MemberDto();
        member.setLoginId(item.getLoginId());
        member.setPassword(passwordEncoder.encode(item.getPassword()));
        member.setMemberName(item.getMemberName());
        member.setRole(item.getRole());
        member.setPhoneNumber(Long.parseLong(item.getPhoneNumber()));
        member.setGender(item.getGender());
        member.setBirth(item.getBirth());
        member.setStatus("ACTIVE");
        member.setUnivId(univId);

        memberMapper.insertMember(member);

        if (item.getDeptId() != null) {
            MemberDto created = memberMapper.findByLoginId(item.getLoginId());
            created.setDeptId(item.getDeptId());
            memberMapper.insertMemberDetail(created);
        }
    }

    // 엑셀 업로드 양식과 동일한 검증 규칙(FE: bulkSignupExcel.ts)을 백엔드에서 한 번 더 적용
    private String validateMemberItem(AdminDto.MemberItemDto item) {
        if (item.getLoginId() == null || !LOGIN_ID_PATTERN.matcher(item.getLoginId()).matches()) {
            return "로그인ID는 숫자만 입력할 수 있습니다.";
        }
        if (item.getPassword() == null || item.getPassword().isBlank()) {
            return "비밀번호를 입력해주세요.";
        }
        if (item.getMemberName() == null || item.getMemberName().isBlank()) {
            return "이름을 입력해주세요.";
        }
        if (item.getPhoneNumber() == null || !PHONE_NUMBER_PATTERN.matcher(item.getPhoneNumber()).matches()) {
            return "휴대폰번호 형식이 올바르지 않습니다.";
        }
        if (!"M".equals(item.getGender()) && !"F".equals(item.getGender())) {
            return "성별은 M 또는 F여야 합니다.";
        }
        if (item.getBirth() == null || !BIRTH_PATTERN.matcher(item.getBirth()).matches()) {
            return "생년월일 형식이 올바르지 않습니다.";
        }
        if (!RoleCode.STU.getCode().equals(item.getRole()) && !RoleCode.PROF.getCode().equals(item.getRole())) {
            return "구분은 학생 또는 교수만 가능합니다.";
        }
        if (RoleCode.PROF.getCode().equals(item.getRole()) && item.getDeptId() == null) {
            return "교수는 학과를 선택해야 합니다.";
        }
        return null;
    }

    @Transactional
    public void updateMemberStatus(AdminDto.MemberStatusDto memberStatusDto) {
        MemberDto member = memberMapper.findByMemberId(memberStatusDto.getMemberId());
        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "회원을 찾을 수 없습니다."
            );
        }
        if ("WITHDRAWN".equals(member.getStatus())
                && !"WITHDRAWN".equals(memberStatusDto.getStatus())) {
            subscriptionMemberLimitService.validateAdditionalMembers(
                    member.getUnivId(),
                    1
            );
        }
        adminMapper.updateMemberStatus(memberStatusDto);
    }

    @Transactional
    public void createNotice(AdminDto.NoticeDto notice, Long requesterId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        notice.setMemberId(requesterId);
        notice.setUnivId(requester.getUnivId());
        adminMapper.insertNotice(notice);
        asyncEmbeddingService.embedNotice(notice);
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

    public AdminDto.HomeConfigDto getHomeConfig(Long univId) {
        AdminDto.HomeConfigDto config = adminMapper.selectHomeConfig(univId);
        if (config == null) {
            config = new AdminDto.HomeConfigDto();
            config.setWeather(true);
            config.setAiChat(true);
            config.setNotice(true);
            config.setMeal(true);
            config.setTel(true);
            config.setShortcut(true);
        }
        return config;
    }

    @Transactional
    public void updateHomeConfig(Long univId, AdminDto.HomeConfigDto dto) {
        adminMapper.insertHomeConfigIfNotExists(univId);
        adminMapper.updateHomeConfig(univId, dto);
    }

    public AdminDto.NoticeConfigDto getNoticeConfig(Long univId) {
        AdminDto.NoticeConfigDto config = adminMapper.selectNoticeConfig(univId);
        if (config == null) {
            config = new AdminDto.NoticeConfigDto();
            config.setDefaultTarget("ALL");
            config.setShowTop(true);
            config.setPushAlert(false);
        }
        return config;
    }

    @Transactional
    public void updateNoticeConfig(Long univId, AdminDto.NoticeConfigDto dto) {
        adminMapper.insertNoticeConfigIfNotExists(univId);
        adminMapper.updateNoticeConfig(univId, dto);
    }

    public List<AdminDto.LectureAssignListDto> getLectureAssignList(Long requesterId, Long univId, Long semId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        Long effectiveUnivId = "SUA".equals(requester.getRole()) ? univId : requester.getUnivId();
        return adminMapper.selectLectureAssignList(effectiveUnivId, semId);
    }

    @Transactional
    public AdminDto.LectureAssignListDto createLectureAssign(AdminDto.LectureAssignCreateDto dto) {
        if (dto.getLecCodeId() == null || dto.getSemId() == null || dto.getProfessorMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "강의·학기·담당 교수는 필수입니다.");
        }
        Long lmsPrfId = adminMapper.selectLmsPrfIdByMemberId(dto.getProfessorMemberId());
        if (lmsPrfId == null) {
            adminMapper.insertLmsProfile(dto.getProfessorMemberId());
            lmsPrfId = adminMapper.selectLmsPrfIdByMemberId(dto.getProfessorMemberId());
        }
        dto.setLmsPrfId(lmsPrfId);
        // 교수 시간표 충돌 차단: 같은 학기에 같은 교수가 같은 요일·겹치는 시간대의 다른 강의를 가질 수 없음
        validateProfessorTimeConflicts(dto, lmsPrfId, null);
        dto.setLecSection(adminMapper.selectNextLectureSection(dto.getSemId(), dto.getLecCodeId()));
        adminMapper.insertLectureAssign(dto);
        if (dto.getTimes() != null) {
            for (AdminDto.LectureTimeDto time : dto.getTimes()) {
                adminMapper.insertLectureTime(dto.getLecId(), time);
            }
        }
        AdminDto.LectureAssignListDto saved = adminMapper.selectLectureAssignById(dto.getLecId());
        asyncEmbeddingService.embedLecture(saved);
        return saved;
    }

    @Transactional
    public AdminDto.LectureAssignListDto updateLectureAssign(Long lecId, AdminDto.LectureAssignCreateDto dto) {
        if (dto.getLecCodeId() == null || dto.getSemId() == null || dto.getProfessorMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "강의·학기·담당 교수는 필수입니다.");
        }
        AdminDto.LectureAssignListDto current = adminMapper.selectLectureAssignById(lecId);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "배정 강의를 찾을 수 없습니다.");
        }
        if (!"OPEN".equals(current.getLecValStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수강신청중 상태의 강의만 수정할 수 있습니다.");
        }
        Long lmsPrfId = adminMapper.selectLmsPrfIdByMemberId(dto.getProfessorMemberId());
        if (lmsPrfId == null) {
            adminMapper.insertLmsProfile(dto.getProfessorMemberId());
            lmsPrfId = adminMapper.selectLmsPrfIdByMemberId(dto.getProfessorMemberId());
        }
        dto.setLmsPrfId(lmsPrfId);
        validateProfessorTimeConflicts(dto, lmsPrfId, lecId); // 자기 자신은 충돌 검사에서 제외
        // 학기·강의가 그대로면 기존 반 유지, 바뀌면 이동한 학기·강의 기준 재채번
        boolean samePlacement = dto.getSemId().equals(current.getSemId())
                && dto.getLecCodeId().equals(current.getLecCodeId());
        dto.setLecSection(samePlacement ? current.getLecSection()
                : adminMapper.selectNextLectureSection(dto.getSemId(), dto.getLecCodeId()));
        dto.setLecId(lecId);
        adminMapper.updateLectureAssign(dto);
        adminMapper.deleteLectureTimes(lecId); // 시간은 전체 교체
        if (dto.getTimes() != null) {
            for (AdminDto.LectureTimeDto time : dto.getTimes()) {
                adminMapper.insertLectureTime(lecId, time);
            }
        }
        AdminDto.LectureAssignListDto updated = adminMapper.selectLectureAssignById(lecId);
        asyncEmbeddingService.embedLecture(updated);
        return updated;
    }

    private void validateProfessorTimeConflicts(AdminDto.LectureAssignCreateDto dto, Long lmsPrfId, Long excludeLecId) {
        if (dto.getTimes() == null) {
            return;
        }
        for (AdminDto.LectureTimeDto time : dto.getTimes()) {
            int conflicts = adminMapper.countProfessorTimeConflicts(
                    lmsPrfId, dto.getSemId(), time.getDayCode(), time.getStartTime(), time.getEndTime(), excludeLecId);
            if (conflicts > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "해당 시간대에 교수의 다른 강의가 있어 배정할 수 없습니다.");
            }
        }
    }

    public List<AdminDto.SemesterDto> getSemesterList() {
        return adminMapper.selectSemesterList();
    }

    public List<AdminDto.ProfessorDto> getProfessorList(Long requesterId, Long univId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        Long effectiveUnivId = "SUA".equals(requester.getRole()) ? univId : requester.getUnivId();
        return adminMapper.selectProfessorList(effectiveUnivId);
    }

    public List<AdminDto.LectureListDto> getLectureList(Long requesterId, Long univId) {
        MemberDto requester = memberMapper.findByMemberId(requesterId);
        Long effectiveUnivId = "SUA".equals(requester.getRole()) ? univId : requester.getUnivId();
        return adminMapper.selectLectureList(effectiveUnivId);
    }

    @Transactional
    public void createLecture(AdminDto.LectureCodeDto dto) {
        validateLectureCode(dto.getDeptId(), dto.getLecCode(), null);
        adminMapper.insertLectureCode(dto);
    }

    @Transactional
    public void updateLecture(AdminDto.LectureCodeDto dto) {
        validateLectureCode(dto.getDeptId(), dto.getLecCode(), dto.getLecCodeId());
        adminMapper.updateLectureCode(dto);
    }

    // 강의 코드 형식(영문 대문자·숫자·하이픈 1~20자, LEC_CODE VARCHAR2(20)) + 같은 학과 내 중복 차단(UNIQUE 위반 시 500 대신 409)
    private void validateLectureCode(Long deptId, String lecCode, Long excludeLecCodeId) {
        if (lecCode == null || !lecCode.matches("[A-Z0-9-]{1,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "강의 코드는 영문 대문자·숫자·하이픈(-) 1~20자여야 합니다.");
        }
        int dup = adminMapper.countLectureCodeDup(deptId, lecCode, excludeLecCodeId);
        if (dup > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "같은 학과에 이미 존재하는 강의 코드입니다.");
        }
    }

    @Transactional
    public void deleteLecture(Long lecCodeId) {
        int assignCount = adminMapper.countLectureAssigns(lecCodeId);
        if (assignCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이 강의로 배정된 강의가 " + assignCount + "건 있어 삭제할 수 없습니다.");
        }
        adminMapper.softDeleteLecture(lecCodeId);
    }
}
