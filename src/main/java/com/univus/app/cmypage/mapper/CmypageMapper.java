package com.univus.app.cmypage.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageSummaryDto;
import com.univus.app.cmypage.dto.CmypageTradeDto;
import com.univus.app.cmypage.dto.CmypageWishlistDto;
import com.univus.app.community.dto.PostDto;
import java.util.List;

@Mapper
public interface CmypageMapper {
    CmypageProfileDto selectMyProfile(Long memberId);
    CmypageSummaryDto selectMySummary(Long memberId);
    List<CmypageCommentDto> selectMyComments(Long memberId);
    List<PostDto> selectLikedPosts(Long memberId);
    List<CmypageTradeDto> selectMyTrades(Long memberId);
    List<CmypageWishlistDto> selectMyWishlist(Long memberId);
    int countCommunityNicknameForOthers(@Param("memberId") Long memberId, @Param("communityNickname") String communityNickname);
    int countMemberDetail(@Param("memberId") Long memberId);
    Long selectDefaultDeptIdForMember(@Param("memberId") Long memberId);
    int updateCommunityNickname(@Param("memberId") Long memberId, @Param("communityNickname") String communityNickname);
    int updateMemberStatus(@Param("memberId") Long memberId, @Param("status") String status);
}
