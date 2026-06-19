package com.univus.app.cmypage.controller;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;
import com.univus.app.cmypage.dto.CmypageSummaryDto;
import com.univus.app.cmypage.dto.CmypageTradeDto;
import com.univus.app.cmypage.dto.CmypageWishlistDto;
import com.univus.app.cmypage.service.CmypageService;
import com.univus.app.cmypage.service.CommunityAccountStatusService;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.community.dto.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cmypage")
@RequiredArgsConstructor
public class CmypageController {

    private final CmypageService cmypageService;
    private final CommunityAccountStatusService communityAccountStatusService;

    @GetMapping("/profile")
    public ResponseEntity<CmypageProfileDto> getMyProfile(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                cmypageService.getMyProfile(requireMemberId(memberId)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<CmypageProfileDto> updateMyProfile(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CmypageProfileUpdateDto request) {
        return ResponseEntity.ok(
                cmypageService.updateMyProfile(
                        requireMemberId(memberId),
                        request));
    }

    @GetMapping("/summary")
    public ResponseEntity<CmypageSummaryDto> getMySummary(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                cmypageService.getMySummary(requireMemberId(memberId)));
    }

    @GetMapping("/posts")
    public ResponseEntity<PaginateUtilRestApiRes<PostDto>> getMyPosts(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "8") Integer size) {
        return ResponseEntity.ok(
                cmypageService.getMyPosts(
                        requireMemberId(memberId),
                        page,
                        size));
    }

    @GetMapping("/comments")
    public ResponseEntity<PaginateUtilRestApiRes<CmypageCommentDto>> getMyComments(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "8") Integer size) {
        return ResponseEntity.ok(
                cmypageService.getMyComments(
                        requireMemberId(memberId),
                        page,
                        size));
    }

    @GetMapping("/liked-posts")
    public ResponseEntity<PaginateUtilRestApiRes<PostDto>> getLikedPosts(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "8") Integer size) {
        return ResponseEntity.ok(
                cmypageService.getLikedPosts(
                        requireMemberId(memberId),
                        page,
                        size));
    }

    @GetMapping("/trades")
    public ResponseEntity<PaginateUtilRestApiRes<CmypageTradeDto>> getMyTrades(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "role", defaultValue = "ALL") String role,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "8") Integer size) {
        return ResponseEntity.ok(
                cmypageService.getMyTrades(
                        requireMemberId(memberId),
                        role,
                        page,
                        size));
    }

    @GetMapping("/wishlist")
    public ResponseEntity<PaginateUtilRestApiRes<CmypageWishlistDto>> getMyWishlist(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "6") Integer size) {
        return ResponseEntity.ok(
                cmypageService.getMyWishlist(
                        requireMemberId(memberId),
                        page,
                        size));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateCommunity(@AuthenticationPrincipal Long memberId) {
        communityAccountStatusService.deactivateCommunity(
                requireMemberId(memberId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reactivate")
    public ResponseEntity<Void> reactivateCommunity(@AuthenticationPrincipal Long memberId) {
        communityAccountStatusService.reactivateCommunity(
                requireMemberId(memberId));
        return ResponseEntity.ok().build();
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다.");
        }
        return memberId;
    }
}
