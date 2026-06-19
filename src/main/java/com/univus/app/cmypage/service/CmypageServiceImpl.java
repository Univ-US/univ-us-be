package com.univus.app.cmypage.service;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;
import com.univus.app.cmypage.dto.CmypageSummaryDto;
import com.univus.app.cmypage.dto.CmypageTradeDto;
import com.univus.app.cmypage.dto.CmypageWishlistDto;
import com.univus.app.cmypage.mapper.CmypageMapper;
import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.community.dto.PostDto;
import com.univus.app.community.dto.PostListResponseDto;
import com.univus.app.community.service.PostListQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CmypageServiceImpl implements CmypageService {

    private final PostListQueryService postListQueryService;
    private final CmypageMapper cmypageMapper;
    private final CmypagePolicy cmypagePolicy;

    @Override
    public PaginateUtilRestApiRes<PostDto> getMyPosts(
            Long memberId,
            Integer page,
            Integer size) {
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        PostDto postDto = new PostDto();
        postDto.setMemberId(memberId);
        postDto.setViewerMemberId(memberId);
        postDto.setPage(safePage + 1);
        postDto.setSize(safeSize);

        PostListResponseDto result =
                postListQueryService.getPostList(postDto);
        return PaginateUtilRestApi.of(
                result.getPostList(),
                result.getTotalCount(),
                safePage,
                safeSize);
    }

    @Override
    public CmypageProfileDto getMyProfile(Long memberId) {
        return cmypagePolicy.requireProfile(
                cmypageMapper.selectMyProfile(memberId));
    }

    @Override
    public CmypageSummaryDto getMySummary(Long memberId) {
        return cmypageMapper.selectMySummary(memberId);
    }

    @Transactional
    @Override
    public CmypageProfileDto updateMyProfile(Long memberId, CmypageProfileUpdateDto request) {
        String communityNickname =
                cmypagePolicy.getValidatedNickname(request);

        int duplicateCount =
                cmypageMapper.countCommunityNicknameForOthers(memberId, communityNickname);
        cmypagePolicy.requireAvailableNickname(duplicateCount);

        boolean hasMemberDetail = cmypageMapper.countMemberDetail(memberId) > 0;
        Long defaultDeptId = null;
        if (!hasMemberDetail) {
            defaultDeptId =
                    cmypageMapper.selectDefaultDeptIdForMember(memberId);
        }
        cmypagePolicy.requireDepartmentAvailable(
                hasMemberDetail,
                defaultDeptId);

        int updated = cmypageMapper.updateCommunityNickname(memberId, communityNickname);
        cmypagePolicy.requireProfileUpdated(updated);

        return getMyProfile(memberId);
    }

    @Override
    public PaginateUtilRestApiRes<CmypageCommentDto> getMyComments(
            Long memberId,
            Integer page,
            Integer size) {
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        List<CmypageCommentDto> comments = cmypageMapper.selectMyComments(
                memberId,
                PaginateUtilRestApi.offset(safePage, safeSize),
                safeSize);
        return PaginateUtilRestApi.of(
                comments,
                cmypageMapper.countMyComments(memberId),
                safePage,
                safeSize);
    }

    @Override
    public PaginateUtilRestApiRes<PostDto> getLikedPosts(
            Long memberId,
            Integer page,
            Integer size) {
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        List<PostDto> posts = cmypageMapper.selectLikedPosts(
                memberId,
                PaginateUtilRestApi.offset(safePage, safeSize),
                safeSize);
        return PaginateUtilRestApi.of(
                posts,
                cmypageMapper.countLikedPosts(memberId),
                safePage,
                safeSize);
    }

    @Override
    public PaginateUtilRestApiRes<CmypageTradeDto> getMyTrades(
            Long memberId,
            String role,
            Integer page,
            Integer size) {
        String normalizedRole = cmypagePolicy.normalizeTradeRole(role);
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        List<CmypageTradeDto> trades = cmypageMapper.selectMyTrades(
                memberId,
                normalizedRole,
                PaginateUtilRestApi.offset(safePage, safeSize),
                safeSize);
        return PaginateUtilRestApi.of(
                trades,
                cmypageMapper.countMyTrades(memberId, normalizedRole),
                safePage,
                safeSize);
    }

    @Override
    public PaginateUtilRestApiRes<CmypageWishlistDto> getMyWishlist(
            Long memberId,
            Integer page,
            Integer size) {
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        List<CmypageWishlistDto> wishlist = cmypageMapper.selectMyWishlist(
                memberId,
                PaginateUtilRestApi.offset(safePage, safeSize),
                safeSize);
        return PaginateUtilRestApi.of(
                wishlist,
                cmypageMapper.countMyWishlist(memberId),
                safePage,
                safeSize);
    }

}
