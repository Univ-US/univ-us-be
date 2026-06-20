package com.univus.app.admin.mapper;

import com.univus.app.admin.dto.SchoolAdminCommunityDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SchoolAdminCommunityMapper {

    List<SchoolAdminCommunityDto.Post> selectPosts(SchoolAdminCommunityDto.PostSearch search);

    long countPosts(SchoolAdminCommunityDto.PostSearch search);

    SchoolAdminCommunityDto.Post selectPostById(@Param("postId") Long postId);

    int updatePostBlind(@Param("postId") Long postId, @Param("isBlind") int isBlind);

    int softDeletePost(@Param("postId") Long postId);

    List<SchoolAdminCommunityDto.MemberReportSummary> selectMembers(SchoolAdminCommunityDto.MemberSearch search);

    long countMembers(SchoolAdminCommunityDto.MemberSearch search);

    SchoolAdminCommunityDto.MemberReportSummary selectMemberById(@Param("memberId") Long memberId);

    int updateMemberStatus(@Param("memberId") Long memberId, @Param("status") String status);
}
