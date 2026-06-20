package com.univus.app.admin.controller;

import com.univus.app.admin.dto.SchoolAdminCommunityDto;
import com.univus.app.admin.service.SchoolAdminCommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-admin/community")
@RequiredArgsConstructor
public class SchoolAdminCommunityController {

    private final SchoolAdminCommunityService schoolAdminCommunityService;

    @GetMapping("/posts")
    public ResponseEntity<SchoolAdminCommunityDto.PostPage> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long boardId,
            @RequestParam(defaultValue = "ALL") String blind,
            @RequestParam(defaultValue = "ALL") String report,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminCommunityService.getPosts(
                        page, keyword, boardId, blind, report, requesterId(authentication))
        );
    }

    @PatchMapping("/posts/{postId}/blind")
    public ResponseEntity<SchoolAdminCommunityDto.Post> changePostBlind(
            @PathVariable Long postId,
            @Valid @RequestBody SchoolAdminCommunityDto.BlindRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminCommunityService.setPostBlind(
                        postId, request.getBlind(), requesterId(authentication))
        );
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Authentication authentication) {
        schoolAdminCommunityService.deletePost(postId, requesterId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members")
    public ResponseEntity<SchoolAdminCommunityDto.MemberPage> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminCommunityService.getMembers(page, keyword, status, requesterId(authentication))
        );
    }

    @PatchMapping("/members/{memberId}/status")
    public ResponseEntity<SchoolAdminCommunityDto.MemberReportSummary> changeMemberStatus(
            @PathVariable Long memberId,
            @Valid @RequestBody SchoolAdminCommunityDto.StatusChangeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminCommunityService.changeMemberStatus(
                        memberId, request.getStatus(), requesterId(authentication))
        );
    }

    private Long requesterId(Authentication authentication) {
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
