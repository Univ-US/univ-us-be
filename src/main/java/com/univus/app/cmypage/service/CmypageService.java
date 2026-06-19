package com.univus.app.cmypage.service;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;
import com.univus.app.cmypage.dto.CmypageSummaryDto;
import com.univus.app.cmypage.dto.CmypageTradeDto;
import com.univus.app.cmypage.dto.CmypageWishlistDto;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.community.dto.PostDto;

public interface CmypageService {

    PaginateUtilRestApiRes<PostDto> getMyPosts(
            Long memberId,
            Integer page,
            Integer size);

    CmypageProfileDto getMyProfile(Long memberId);

    CmypageSummaryDto getMySummary(Long memberId);

    CmypageProfileDto updateMyProfile(
            Long memberId,
            CmypageProfileUpdateDto request);

    PaginateUtilRestApiRes<CmypageCommentDto> getMyComments(
            Long memberId,
            Integer page,
            Integer size);

    PaginateUtilRestApiRes<PostDto> getLikedPosts(
            Long memberId,
            Integer page,
            Integer size);

    PaginateUtilRestApiRes<CmypageTradeDto> getMyTrades(
            Long memberId,
            String role,
            Integer page,
            Integer size);

    PaginateUtilRestApiRes<CmypageWishlistDto> getMyWishlist(
            Long memberId,
            Integer page,
            Integer size);
}
