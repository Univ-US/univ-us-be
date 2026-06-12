package com.univus.app.cmypage.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.dto.CmypageTradeDto;
import com.univus.app.cmypage.dto.CmypageWishlistDto;
import com.univus.app.community.dto.PostDto;
import java.util.List;

@Mapper
public interface CmypageMapper {
    List<CmypageCommentDto> selectMyComments(Long memberId);
    List<PostDto> selectLikedPosts(Long memberId);
    List<CmypageTradeDto> selectMyTrades(Long memberId);
    List<CmypageWishlistDto> selectMyWishlist(Long memberId);
    int updateMemberStatus(@org.apache.ibatis.annotations.Param("memberId") Long memberId, @org.apache.ibatis.annotations.Param("status") String status);
}
