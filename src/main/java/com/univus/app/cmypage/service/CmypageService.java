package com.univus.app.cmypage.service;

import com.univus.app.cmypage.dto.CmypageCommentDto;
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
