package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminCommunityDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceAdminCommunityMapper {

    List<ServiceAdminCommunityDto.Post> selectPosts(ServiceAdminCommunityDto.PostSearch search);

    long countPosts(ServiceAdminCommunityDto.PostSearch search);

    ServiceAdminCommunityDto.Post selectPostById(@Param("postId") Long postId);

    int updatePostBlind(@Param("postId") Long postId, @Param("isBlind") int isBlind);

    int softDeletePost(@Param("postId") Long postId);

    List<ServiceAdminCommunityDto.MemberReportSummary> selectMembers(ServiceAdminCommunityDto.MemberSearch search);

    long countMembers(ServiceAdminCommunityDto.MemberSearch search);

    ServiceAdminCommunityDto.MemberReportSummary selectMemberById(@Param("memberId") Long memberId);

    int updateMemberStatus(@Param("memberId") Long memberId, @Param("status") String status);
}
