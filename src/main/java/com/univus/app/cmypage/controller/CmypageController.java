package com.univus.app.cmypage.controller;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;
import com.univus.app.cmypage.dto.CmypageSummaryDto;
import com.univus.app.cmypage.dto.CmypageTradeDto;
import com.univus.app.cmypage.dto.CmypageWishlistDto;
import com.univus.app.cmypage.service.CmypageService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cmypage")
@RequiredArgsConstructor
public class CmypageController {

    private final CmypageService cmypageService;

    @GetMapping("/profile")
    public ResponseEntity<CmypageProfileDto> getMyProfile(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return ResponseEntity.ok(cmypageService.getMyProfile(memberId));
    }

    @PatchMapping("/profile")
    public ResponseEntity<CmypageProfileDto> updateMyProfile(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CmypageProfileUpdateDto request) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return ResponseEntity.ok(cmypageService.updateMyProfile(memberId, request));
    }

    @GetMapping("/summary")
    public ResponseEntity<CmypageSummaryDto> getMySummary(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return ResponseEntity.ok(cmypageService.getMySummary(memberId));
    }

    @GetMapping("/posts")
    public ResponseEntity<Map<String, Object>> getMyPosts(
            @AuthenticationPrincipal Long memberId,
            PostDto postDto) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        Map<String, Object> result = cmypageService.getMyPosts(memberId, postDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CmypageCommentDto>> getMyComments(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        List<CmypageCommentDto> result = cmypageService.getMyComments(memberId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/liked-posts")
    public ResponseEntity<List<PostDto>> getLikedPosts(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        List<PostDto> result = cmypageService.getLikedPosts(memberId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trades")
    public ResponseEntity<List<CmypageTradeDto>> getMyTrades(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        List<CmypageTradeDto> result = cmypageService.getMyTrades(memberId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<CmypageWishlistDto>> getMyWishlist(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        List<CmypageWishlistDto> result = cmypageService.getMyWishlist(memberId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateCommunity(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        cmypageService.deactivateCommunity(memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reactivate")
    public ResponseEntity<Void> reactivateCommunity(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        cmypageService.reactivateCommunity(memberId);
        return ResponseEntity.ok().build();
    }
}
