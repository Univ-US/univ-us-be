package com.univus.app.serviceadmin.service;

import com.univus.app.member.service.RefreshTokenService;
import com.univus.app.serviceadmin.dto.ServiceAdminMemberDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceAdminMemberServiceImpl implements ServiceAdminMemberService {

    private static final int PAGE_SIZE = 10;
    private static final int DETAIL_PAGE_SIZE = 10;
    private static final int MAX_DETAIL_PAGE_SIZE = 50;
    private static final Set<String> FILTER_STATUSES =
            Set.of("ACTIVE", "SUSPENDED", "WITHDRAWN");
    private static final Set<String> MUTABLE_STATUSES =
            Set.of("ACTIVE", "SUSPENDED");
    private static final Set<String> USER_MUTABLE_STATUSES =
            Set.of("ACTIVE", "SUSPENDED", "WITHDRAWN");
    private static final Set<String> USER_ROLES =
            Set.of("GUEST", "STU", "PROF", "ALU");
    private static final Set<String> SORT_OPTIONS =
            Set.of("SCHOOL_ASC", "NAME_ASC", "JOINED_DESC");
    private static final Set<String> USER_SORT_OPTIONS =
            Set.of("JOINED_DESC", "JOINED_ASC", "NAME_ASC", "SCHOOL_ASC", "ROLE_ASC", "LOGIN_DESC");
    private static final Set<String> USER_ACTIVITY_TYPES =
            Set.of("POSTS", "COMMENTS", "COURSES", "SUBMISSIONS", "RESERVATIONS", "PENALTIES", "INQUIRIES");
    private static final Set<String> MEMBER_ACTIVITY_TYPES =
            Set.of("NOTICES", "POSTS", "COMMENTS", "INQUIRIES");

    private final ServiceAdminMemberMapper serviceAdminMemberMapper;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.Page getMembers(
            int page,
            String keyword,
            String status,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        ServiceAdminMemberDto.Search search = new ServiceAdminMemberDto.Search();
        search.setKeyword(normalizeKeyword(keyword));
        search.setStatus(normalizeFilterStatus(status));
        search.setSort(normalizeSort(sort));
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminMemberDto.Member> members =
                serviceAdminMemberMapper.selectMembers(search);
        long totalElements = serviceAdminMemberMapper.countMembers(search);
        ServiceAdminMemberDto.Stats stats =
                serviceAdminMemberMapper.selectMemberStats();

        return new ServiceAdminMemberDto.Page(
                members,
                safePage,
                PAGE_SIZE,
                totalElements,
                stats
        );
    }

    @Transactional
    @Override
    public ServiceAdminMemberDto.Member changeStatus(
            Long memberId,
            String requestedStatus
    ) {
        String status = normalizeMutableStatus(requestedStatus);
        ServiceAdminMemberDto.Member member =
                serviceAdminMemberMapper.selectMemberById(memberId);

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "학교 관리자 계정을 찾을 수 없습니다."
            );
        }
        if ("WITHDRAWN".equals(member.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "탈퇴한 학교 관리자 계정의 상태는 변경할 수 없습니다."
            );
        }
        if (status.equals(member.getStatus())) {
            return member;
        }

        int updated = serviceAdminMemberMapper.updateMemberStatus(memberId, status);
        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "학교 관리자 계정 상태를 변경하지 못했습니다."
            );
        }

        if (!"ACTIVE".equals(status)) {
            refreshTokenService.deleteMemberSessions(memberId);
        }

        return serviceAdminMemberMapper.selectMemberById(memberId);
    }

    @Transactional
    @Override
    public ServiceAdminMemberDto.ForceLogoutResponse forceLogoutMember(Long memberId) {
        ServiceAdminMemberDto.Member member =
                serviceAdminMemberMapper.selectMemberById(memberId);

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "?숆탳 愿由ъ옄 怨꾩젙??李얠쓣 ???놁뒿?덈떎."
            );
        }

        int deletedCount = refreshTokenService.deleteMemberSessions(memberId);
        return new ServiceAdminMemberDto.ForceLogoutResponse(memberId, deletedCount);
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.MemberDetail getMemberDetail(Long memberId) {
        ServiceAdminMemberDto.Member member =
                serviceAdminMemberMapper.selectMemberById(memberId);

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "?숆탳 愿由ъ옄 怨꾩젙??李얠쓣 ???놁뒿?덈떎."
            );
        }

        return new ServiceAdminMemberDto.MemberDetail(
                member,
                serviceAdminMemberMapper.selectMemberActivitySummary(memberId),
                serviceAdminMemberMapper.selectMemberLoginLogs(memberId)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.UserLoginLogPage getMemberLoginLogs(
            Long memberId,
            int page,
            int size
    ) {
        if (serviceAdminMemberMapper.selectMemberById(memberId) == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "학교 관리자 계정을 찾을 수 없습니다."
            );
        }

        int safePage = Math.max(page, 0);
        int safeSize = normalizeDetailSize(size);
        int offset = safePage * safeSize;
        return new ServiceAdminMemberDto.UserLoginLogPage(
                serviceAdminMemberMapper.selectMemberLoginLogsPage(memberId, offset, safeSize),
                safePage,
                safeSize,
                serviceAdminMemberMapper.countMemberLoginLogs(memberId)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.UserActivityItemPage getMemberActivities(
            Long memberId,
            String activityType,
            int page,
            int size
    ) {
        if (serviceAdminMemberMapper.selectMemberById(memberId) == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "학교 관리자 계정을 찾을 수 없습니다."
            );
        }

        String normalizedType = normalizeMemberActivityType(activityType);
        int safePage = Math.max(page, 0);
        int safeSize = normalizeDetailSize(size);
        int offset = safePage * safeSize;

        List<ServiceAdminMemberDto.UserActivityItem> content;
        long totalElements;
        switch (normalizedType) {
            case "NOTICES" -> {
                content = serviceAdminMemberMapper.selectMemberNoticeActivities(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countMemberNoticeActivities(memberId);
            }
            case "POSTS" -> {
                content = serviceAdminMemberMapper.selectMemberPostActivities(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countMemberPostActivities(memberId);
            }
            case "COMMENTS" -> {
                content = serviceAdminMemberMapper.selectMemberCommentActivities(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countMemberCommentActivities(memberId);
            }
            case "INQUIRIES" -> {
                content = serviceAdminMemberMapper.selectMemberInquiryActivities(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countMemberInquiryActivities(memberId);
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 활동 유형입니다."
            );
        }

        return new ServiceAdminMemberDto.UserActivityItemPage(
                content,
                safePage,
                safeSize,
                totalElements
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.UserPage getUsers(
            int page,
            String keyword,
            Long univId,
            String role,
            String status,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        ServiceAdminMemberDto.UserSearch search = new ServiceAdminMemberDto.UserSearch();
        search.setKeyword(normalizeKeyword(keyword));
        search.setUnivId(univId);
        search.setRole(normalizeRole(role));
        search.setStatus(normalizeFilterStatus(status));
        search.setSort(normalizeUserSort(sort));
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminMemberDto.User> users =
                serviceAdminMemberMapper.selectUsers(search);
        long totalElements = serviceAdminMemberMapper.countUsers(search);
        ServiceAdminMemberDto.UserStats stats =
                serviceAdminMemberMapper.selectUserStats();

        return new ServiceAdminMemberDto.UserPage(
                users,
                safePage,
                PAGE_SIZE,
                totalElements,
                stats
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.UserDetail getUserDetail(Long memberId) {
        ServiceAdminMemberDto.User user = getUserOrThrow(memberId);
        return new ServiceAdminMemberDto.UserDetail(
                user,
                serviceAdminMemberMapper.selectUserActivitySummary(memberId),
                serviceAdminMemberMapper.selectUserLoginLogs(memberId),
                serviceAdminMemberMapper.selectUserCommunityActivities(memberId),
                serviceAdminMemberMapper.selectUserCourseActivities(memberId),
                serviceAdminMemberMapper.selectUserSubmissionActivities(memberId),
                serviceAdminMemberMapper.selectUserReservationActivities(memberId),
                serviceAdminMemberMapper.selectUserInquiryActivities(memberId)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.UserLoginLogPage getUserLoginLogs(
            Long memberId,
            int page,
            int size
    ) {
        getUserOrThrow(memberId);
        int safePage = Math.max(page, 0);
        int safeSize = normalizeDetailSize(size);
        int offset = safePage * safeSize;
        return new ServiceAdminMemberDto.UserLoginLogPage(
                serviceAdminMemberMapper.selectUserLoginLogsPage(memberId, offset, safeSize),
                safePage,
                safeSize,
                serviceAdminMemberMapper.countUserLoginLogs(memberId)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminMemberDto.UserActivityItemPage getUserActivities(
            Long memberId,
            String activityType,
            int page,
            int size
    ) {
        getUserOrThrow(memberId);
        String normalizedType = normalizeUserActivityType(activityType);
        int safePage = Math.max(page, 0);
        int safeSize = normalizeDetailSize(size);
        int offset = safePage * safeSize;

        List<ServiceAdminMemberDto.UserActivityItem> content;
        long totalElements;
        switch (normalizedType) {
            case "POSTS" -> {
                content = serviceAdminMemberMapper.selectUserPostActivities(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countUserPostActivities(memberId);
            }
            case "COMMENTS" -> {
                content = serviceAdminMemberMapper.selectUserCommentActivities(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countUserCommentActivities(memberId);
            }
            case "COURSES" -> {
                content = serviceAdminMemberMapper.selectUserCourseActivitiesPage(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countUserCourseActivities(memberId);
            }
            case "SUBMISSIONS" -> {
                content = serviceAdminMemberMapper.selectUserSubmissionActivitiesPage(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countUserSubmissionActivities(memberId);
            }
            case "RESERVATIONS" -> {
                content = serviceAdminMemberMapper.selectUserReservationActivitiesPage(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countUserReservationActivities(memberId);
            }
            case "PENALTIES" -> {
                content = serviceAdminMemberMapper.selectUserPenaltyActivities(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countUserPenaltyActivities(memberId);
            }
            case "INQUIRIES" -> {
                content = serviceAdminMemberMapper.selectUserInquiryActivitiesPage(memberId, offset, safeSize);
                totalElements = serviceAdminMemberMapper.countUserInquiryActivities(memberId);
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 활동 유형입니다."
            );
        }

        return new ServiceAdminMemberDto.UserActivityItemPage(
                content,
                safePage,
                safeSize,
                totalElements
        );
    }

    @Transactional
    @Override
    public ServiceAdminMemberDto.User changeUserStatus(
            Long memberId,
            String requestedStatus
    ) {
        String status = normalizeUserMutableStatus(requestedStatus);
        ServiceAdminMemberDto.User user = getUserOrThrow(memberId);

        if (status.equals(user.getStatus())) {
            return user;
        }

        int updated = serviceAdminMemberMapper.updateUserStatus(memberId, status);
        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이용자 상태를 변경하지 못했습니다."
            );
        }

        if (!"ACTIVE".equals(status)) {
            refreshTokenService.deleteMemberSessions(memberId);
        }

        return serviceAdminMemberMapper.selectUserById(memberId);
    }

    @Transactional
    @Override
    public ServiceAdminMemberDto.ForceLogoutResponse forceLogoutUser(Long memberId) {
        getUserOrThrow(memberId);
        int deletedCount = refreshTokenService.deleteMemberSessions(memberId);
        return new ServiceAdminMemberDto.ForceLogoutResponse(memberId, deletedCount);
    }

    private ServiceAdminMemberDto.User getUserOrThrow(Long memberId) {
        ServiceAdminMemberDto.User user =
                serviceAdminMemberMapper.selectUserById(memberId);
        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이용자를 찾을 수 없습니다."
            );
        }
        return user;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeFilterStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!FILTER_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 회원 상태입니다."
            );
        }
        return normalized;
    }

    private String normalizeMutableStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!MUTABLE_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "회원 상태는 ACTIVE 또는 SUSPENDED로만 변경할 수 있습니다."
            );
        }
        return normalized;
    }

    private String normalizeUserMutableStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!USER_MUTABLE_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이용자 상태는 ACTIVE, SUSPENDED, WITHDRAWN으로만 변경할 수 있습니다."
            );
        }
        return normalized;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (!USER_ROLES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 이용자 역할입니다."
            );
        }
        return normalized;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "SCHOOL_ASC";
        }
        String normalized = sort.trim().toUpperCase();
        if (!SORT_OPTIONS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다."
            );
        }
        return normalized;
    }

    private String normalizeUserSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "JOINED_DESC";
        }
        String normalized = sort.trim().toUpperCase();
        if (!USER_SORT_OPTIONS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다."
            );
        }
        return normalized;
    }

    private int normalizeDetailSize(int size) {
        if (size <= 0) {
            return DETAIL_PAGE_SIZE;
        }
        return Math.min(size, MAX_DETAIL_PAGE_SIZE);
    }

    private String normalizeUserActivityType(String activityType) {
        if (activityType == null || activityType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "활동 유형이 필요합니다."
            );
        }
        String normalized = activityType.trim().toUpperCase();
        if (!USER_ACTIVITY_TYPES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 활동 유형입니다."
            );
        }
        return normalized;
    }

    private String normalizeMemberActivityType(String activityType) {
        if (activityType == null || activityType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "활동 유형이 필요합니다."
            );
        }
        String normalized = activityType.trim().toUpperCase();
        if (!MEMBER_ACTIVITY_TYPES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 활동 유형입니다."
            );
        }
        return normalized;
    }
}
