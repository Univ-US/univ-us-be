package com.univus.app.cmypage.service;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;
import com.univus.app.cmypage.dto.CmypageSummaryDto;
import com.univus.app.cmypage.dto.CmypageTradeDto;
import com.univus.app.cmypage.dto.CmypageWishlistDto;
import com.univus.app.cmypage.mapper.CmypageMapper;
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

    public Map<String, Object> getMyPosts(Long memberId, PostDto postDto) {
        postDto.setMemberId(memberId);
        return postService.getPostList(postDto);
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

        int updated = cmypageMapper.updateCommunityNickname(memberId, communityNickname);
        if (updated <= 0) {
            throw new IllegalArgumentException("회원 상세 정보를 찾을 수 없습니다.");
        }

        return getMyProfile(memberId);
    }

    public List<CmypageCommentDto> getMyComments(Long memberId) {
        return cmypageMapper.selectMyComments(memberId);
    }

    public List<PostDto> getLikedPosts(Long memberId) {
        return cmypageMapper.selectLikedPosts(memberId);
    }

    public List<CmypageTradeDto> getMyTrades(Long memberId) {
        return cmypageMapper.selectMyTrades(memberId);
    }

    public List<CmypageWishlistDto> getMyWishlist(Long memberId) {
        return cmypageMapper.selectMyWishlist(memberId);
    }
}
