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
import com.univus.app.community.service.PostService;
import com.univus.app.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CmypageService {

    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;

    private final PostService postService;
    private final CmypageMapper cmypageMapper;
    private final ReservationService reservationService;

    @Transactional
    public void deactivateCommunity(Long memberId) {
        cmypageMapper.updateMemberStatus(memberId, "INACTIVE");
        reservationService.cancelAllPendingReservations(memberId);
    }

    @Transactional
    public void reactivateCommunity(Long memberId) {
        cmypageMapper.updateMemberStatus(memberId, "ACTIVE");
    }

    @SuppressWarnings("unchecked")
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

        Map<String, Object> result = postService.getPostList(postDto);
        List<PostDto> posts = (List<PostDto>) result.getOrDefault("postList", List.of());
        Number totalCount = (Number) result.getOrDefault("totalCount", 0);
        return PaginateUtilRestApi.of(posts, totalCount.longValue(), safePage, safeSize);
    }

    public CmypageProfileDto getMyProfile(Long memberId) {
        CmypageProfileDto profile = cmypageMapper.selectMyProfile(memberId);
        if (profile == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }
        return profile;
    }

    public CmypageSummaryDto getMySummary(Long memberId) {
        return cmypageMapper.selectMySummary(memberId);
    }

    @Transactional
    public CmypageProfileDto updateMyProfile(Long memberId, CmypageProfileUpdateDto request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 필요합니다.");
        }

        String communityNickname = request.getCommunityNickname();
        if (communityNickname == null) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }

        communityNickname = communityNickname.trim();
        if (communityNickname.length() < MIN_NICKNAME_LENGTH
                || communityNickname.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("닉네임은 2자 이상 20자 이하로 입력해주세요.");
        }

        int duplicateCount =
                cmypageMapper.countCommunityNicknameForOthers(memberId, communityNickname);
        if (duplicateCount > 0) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        boolean hasMemberDetail = cmypageMapper.countMemberDetail(memberId) > 0;
        if (!hasMemberDetail && cmypageMapper.selectDefaultDeptIdForMember(memberId) == null) {
            throw new IllegalStateException("소속 대학의 학과 정보가 없어 커뮤니티 닉네임을 설정할 수 없습니다.");
        }

        int updated = cmypageMapper.updateCommunityNickname(memberId, communityNickname);
        if (updated <= 0) {
            throw new IllegalArgumentException("회원 상세 정보를 찾을 수 없습니다.");
        }

        return getMyProfile(memberId);
    }

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

    public PaginateUtilRestApiRes<CmypageTradeDto> getMyTrades(
            Long memberId,
            String role,
            Integer page,
            Integer size) {
        String normalizedRole = normalizeTradeRole(role);
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

    private String normalizeTradeRole(String role) {
        if (role == null) {
            return "ALL";
        }

        String normalizedRole = role.trim().toUpperCase();
        return switch (normalizedRole) {
            case "SELLER", "BUYER" -> normalizedRole;
            default -> "ALL";
        };
    }
}
