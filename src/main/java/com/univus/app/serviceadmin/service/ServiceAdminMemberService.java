package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminMemberDto;

public interface ServiceAdminMemberService {

    ServiceAdminMemberDto.Page getMembers(
            int page,
            String keyword,
            String status,
            String sort
    );

    ServiceAdminMemberDto.Member changeStatus(
            Long memberId,
            String requestedStatus
    );

    ServiceAdminMemberDto.ForceLogoutResponse forceLogoutMember(Long memberId);

    ServiceAdminMemberDto.MemberDetail getMemberDetail(Long memberId);

    ServiceAdminMemberDto.UserLoginLogPage getMemberLoginLogs(
            Long memberId,
            int page,
            int size
    );

    ServiceAdminMemberDto.UserActivityItemPage getMemberActivities(
            Long memberId,
            String activityType,
            int page,
            int size
    );

    ServiceAdminMemberDto.UserPage getUsers(
            int page,
            String keyword,
            Long univId,
            String role,
            String status,
            String sort
    );

    ServiceAdminMemberDto.UserDetail getUserDetail(Long memberId);

    ServiceAdminMemberDto.UserLoginLogPage getUserLoginLogs(
            Long memberId,
            int page,
            int size
    );

    ServiceAdminMemberDto.UserActivityItemPage getUserActivities(
            Long memberId,
            String activityType,
            int page,
            int size
    );

    ServiceAdminMemberDto.User changeUserStatus(
            Long memberId,
            String requestedStatus
    );

    ServiceAdminMemberDto.ForceLogoutResponse forceLogoutUser(Long memberId);
}
