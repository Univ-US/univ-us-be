package com.univus.app.serviceadmin.service;

import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import com.univus.app.serviceadmin.dto.ServiceAdminBulkSignupDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminBulkSignupMapper;
import com.univus.app.subscription.service.SubscriptionMemberLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ServiceAdminBulkSignupServiceImpl implements ServiceAdminBulkSignupService {

    private static final int MAX_BULK_MEMBERS = 500;
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^010\\d{8}$");
    private static final Pattern BIRTH_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Set<String> MEMBER_ROLES = Set.of("STU", "PROF");

    private final ServiceAdminBulkSignupMapper bulkSignupMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionMemberLimitService subscriptionMemberLimitService;

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminBulkSignupDto.PrecheckResponse precheck(
            ServiceAdminBulkSignupDto.Request request
    ) {
        return evaluate(request);
    }

    @Transactional
    @Override
    public ServiceAdminBulkSignupDto.RegisterResponse register(
            ServiceAdminBulkSignupDto.Request request
    ) {
        ServiceAdminBulkSignupDto.PrecheckResponse precheck = evaluate(request);
        if (!precheck.isCanRegister()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    precheck.getReason()
            );
        }

        List<ServiceAdminBulkSignupDto.MemberInput> members = request.getMembers();
        subscriptionMemberLimitService.validateAdditionalMembers(
                request.getUnivId(),
                members.size()
        );

        for (ServiceAdminBulkSignupDto.MemberInput member : members) {
            insertMember(member, request.getUnivId());
        }

        return new ServiceAdminBulkSignupDto.RegisterResponse(members.size());
    }

    private ServiceAdminBulkSignupDto.PrecheckResponse evaluate(
            ServiceAdminBulkSignupDto.Request request
    ) {
        if (request == null || request.getUnivId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 학교를 선택해 주세요.");
        }

        List<ServiceAdminBulkSignupDto.MemberInput> members = request.getMembers();
        if (members == null || members.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "등록할 이용자가 없습니다.");
        }
        if (members.size() > MAX_BULK_MEMBERS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "한 번에 등록할 수 있는 이용자는 최대 " + MAX_BULK_MEMBERS + "명입니다."
            );
        }

        ServiceAdminBulkSignupDto.Capacity capacity =
                bulkSignupMapper.selectActiveCapacity(request.getUnivId());
        if (capacity == null) {
            return unavailableResponse(members.size(), "선택한 학교는 활성 구독 상태가 아닙니다.");
        }

        List<ServiceAdminBulkSignupDto.ValidationError> errors = validateMembers(
                members,
                request.getUnivId()
        );
        long currentMemberCount = valueOrZero(capacity.getCurrentMemberCount());
        long maxMemberCount = valueOrZero(capacity.getMaxMemberCount());
        long remainingMemberCount = Math.max(maxMemberCount - currentMemberCount, 0);
        boolean capacityExceeded = currentMemberCount + members.size() > maxMemberCount;

        String reason = null;
        if (!errors.isEmpty()) {
            reason = "엑셀 데이터에 오류가 있어 등록할 수 없습니다.";
        } else if (capacityExceeded) {
            reason = "현재 이용자 수와 등록 예정 인원을 합하면 플랜 이용자 한도를 초과합니다.";
        }

        return new ServiceAdminBulkSignupDto.PrecheckResponse(
                reason == null,
                reason,
                capacity.getUnivName(),
                capacity.getPlanName(),
                currentMemberCount,
                maxMemberCount,
                members.size(),
                remainingMemberCount,
                errors
        );
    }

    private List<ServiceAdminBulkSignupDto.ValidationError> validateMembers(
            List<ServiceAdminBulkSignupDto.MemberInput> members,
            Long univId
    ) {
        List<ServiceAdminBulkSignupDto.ValidationError> errors = new ArrayList<>();
        Map<Integer, List<String>> messagesByRow = new LinkedHashMap<>();
        Set<String> requestLoginIds = new HashSet<>();
        Set<String> uniqueLoginIds = new HashSet<>();
        Set<Long> departmentIds = new HashSet<>(
                bulkSignupMapper.selectDepartmentIdsByUniversity(univId)
        );

        for (int index = 0; index < members.size(); index++) {
            ServiceAdminBulkSignupDto.MemberInput member = members.get(index);
            int rowNumber = index + 1;
            if (member == null) {
                addError(messagesByRow, rowNumber, "빈 행은 등록할 수 없습니다.");
                continue;
            }

            validateMemberFields(member, departmentIds, rowNumber, messagesByRow);
            String loginId = member.getLoginId();
            if (loginId != null && LOGIN_ID_PATTERN.matcher(loginId).matches()) {
                if (!requestLoginIds.add(loginId)) {
                    addError(messagesByRow, rowNumber, "파일 안에 중복된 로그인 ID가 있습니다.");
                }
                uniqueLoginIds.add(loginId);
            }
        }

        if (!uniqueLoginIds.isEmpty()) {
            Set<String> existingLoginIds = new HashSet<>(
                    bulkSignupMapper.selectExistingLoginIds(new ArrayList<>(uniqueLoginIds))
            );
            for (int index = 0; index < members.size(); index++) {
                ServiceAdminBulkSignupDto.MemberInput member = members.get(index);
                if (member != null && existingLoginIds.contains(member.getLoginId())) {
                    addError(messagesByRow, index + 1, "이미 사용 중인 로그인 ID입니다.");
                }
            }
        }

        messagesByRow.forEach((rowNumber, messages) -> messages.forEach(message ->
                errors.add(new ServiceAdminBulkSignupDto.ValidationError(rowNumber, message))
        ));
        return errors;
    }

    private void validateMemberFields(
            ServiceAdminBulkSignupDto.MemberInput member,
            Set<Long> departmentIds,
            int rowNumber,
            Map<Integer, List<String>> messagesByRow
    ) {
        if (member.getLoginId() == null || !LOGIN_ID_PATTERN.matcher(member.getLoginId()).matches()) {
            addError(messagesByRow, rowNumber, "로그인 ID는 숫자만 입력할 수 있습니다.");
        }
        if (member.getPassword() == null || member.getPassword().isBlank()) {
            addError(messagesByRow, rowNumber, "초기 비밀번호가 없습니다.");
        }
        if (member.getMemberName() == null || member.getMemberName().isBlank()) {
            addError(messagesByRow, rowNumber, "이름을 입력해 주세요.");
        }
        if (member.getPhoneNumber() == null || !PHONE_NUMBER_PATTERN.matcher(member.getPhoneNumber()).matches()) {
            addError(messagesByRow, rowNumber, "전화번호는 01012345678 형식이어야 합니다.");
        }
        if (!"M".equals(member.getGender()) && !"F".equals(member.getGender())) {
            addError(messagesByRow, rowNumber, "성별은 M 또는 F여야 합니다.");
        }
        if (member.getBirth() == null || !BIRTH_PATTERN.matcher(member.getBirth()).matches()) {
            addError(messagesByRow, rowNumber, "생년월일은 YYYYMMDD 형식이어야 합니다.");
        }
        if (!MEMBER_ROLES.contains(member.getRole())) {
            addError(messagesByRow, rowNumber, "구분은 학생 또는 교수만 가능합니다.");
            return;
        }
        if ("PROF".equals(member.getRole()) && member.getDeptId() == null) {
            addError(messagesByRow, rowNumber, "교수는 학과를 선택해야 합니다.");
        }
        if (member.getDeptId() != null && !departmentIds.contains(member.getDeptId())) {
            addError(messagesByRow, rowNumber, "선택한 학교에 없는 학과입니다.");
        }
    }

    private void insertMember(
            ServiceAdminBulkSignupDto.MemberInput input,
            Long univId
    ) {
        MemberDto member = new MemberDto();
        member.setLoginId(input.getLoginId());
        member.setPassword(passwordEncoder.encode(input.getPassword()));
        member.setMemberName(input.getMemberName());
        member.setRole(input.getRole());
        member.setPhoneNumber(Long.parseLong(input.getPhoneNumber()));
        member.setGender(input.getGender());
        member.setBirth(input.getBirth());
        member.setStatus("ACTIVE");
        member.setUnivId(univId);
        if (memberMapper.insertMember(member) != 1) {
            throw new IllegalStateException("이용자 등록에 실패했습니다.");
        }

        if (input.getDeptId() != null) {
            MemberDto createdMember = memberMapper.findByLoginId(input.getLoginId());
            if (createdMember == null) {
                throw new IllegalStateException("등록한 이용자 정보를 찾을 수 없습니다.");
            }
            createdMember.setDeptId(input.getDeptId());
            if (memberMapper.insertMemberDetail(createdMember) != 1) {
                throw new IllegalStateException("이용자 학과 정보 등록에 실패했습니다.");
            }
        }
    }

    private ServiceAdminBulkSignupDto.PrecheckResponse unavailableResponse(
            int requestedMemberCount,
            String reason
    ) {
        return new ServiceAdminBulkSignupDto.PrecheckResponse(
                false,
                reason,
                null,
                null,
                0,
                0,
                requestedMemberCount,
                0,
                List.of()
        );
    }

    private void addError(
            Map<Integer, List<String>> messagesByRow,
            int rowNumber,
            String message
    ) {
        messagesByRow.computeIfAbsent(rowNumber, ignored -> new ArrayList<>()).add(message);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
