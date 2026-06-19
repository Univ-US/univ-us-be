package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceAdminMemberMapper {

    List<ServiceAdminMemberDto.Member> selectMembers(
            ServiceAdminMemberDto.Search search
    );

    long countMembers(ServiceAdminMemberDto.Search search);

    ServiceAdminMemberDto.Stats selectMemberStats();

    ServiceAdminMemberDto.Member selectMemberById(
            @Param("memberId") Long memberId
    );

    ServiceAdminMemberDto.MemberActivitySummary selectMemberActivitySummary(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserLoginLog> selectMemberLoginLogs(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserLoginLog> selectMemberLoginLogsPage(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countMemberLoginLogs(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectMemberNoticeActivities(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countMemberNoticeActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectMemberPostActivities(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countMemberPostActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectMemberCommentActivities(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countMemberCommentActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectMemberInquiryActivities(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countMemberInquiryActivities(
            @Param("memberId") Long memberId
    );

    int updateMemberStatus(
            @Param("memberId") Long memberId,
            @Param("status") String status
    );

    List<ServiceAdminMemberDto.User> selectUsers(
            ServiceAdminMemberDto.UserSearch search
    );

    long countUsers(ServiceAdminMemberDto.UserSearch search);

    ServiceAdminMemberDto.UserStats selectUserStats();

    ServiceAdminMemberDto.User selectUserById(
            @Param("memberId") Long memberId
    );

    ServiceAdminMemberDto.UserActivitySummary selectUserActivitySummary(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserLoginLog> selectUserLoginLogs(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserLoginLog> selectUserLoginLogsPage(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserLoginLogs(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserCommunityActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserPostActivities(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserPostActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserCommentActivities(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserCommentActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserCourseActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserCourseActivitiesPage(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserCourseActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserSubmissionActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserSubmissionActivitiesPage(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserSubmissionActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserReservationActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserReservationActivitiesPage(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserReservationActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserPenaltyActivities(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserPenaltyActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserInquiryActivities(
            @Param("memberId") Long memberId
    );

    List<ServiceAdminMemberDto.UserActivityItem> selectUserInquiryActivitiesPage(
            @Param("memberId") Long memberId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countUserInquiryActivities(
            @Param("memberId") Long memberId
    );

    int updateUserStatus(
            @Param("memberId") Long memberId,
            @Param("status") String status
    );
}
