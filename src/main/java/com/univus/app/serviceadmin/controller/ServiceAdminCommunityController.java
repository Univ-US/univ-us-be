package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminCommunityDto;
import com.univus.app.serviceadmin.service.ServiceAdminCommunityService;
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
public class ServiceAdminCommunityController {

    private final ServiceAdminCommunityService serviceAdminCommunityService;

    @GetMapping("/posts")
    public ResponseEntity<ServiceAdminCommunityDto.PostPage> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long boardId,
            @RequestParam(defaultValue = "ALL") String blind,
            @RequestParam(defaultValue = "ALL") String report,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminCommunityService.getPosts(
                        page, keyword, boardId, blind, report, requesterId(authentication))
        );
    }

    @PatchMapping("/posts/{postId}/blind")
    public ResponseEntity<ServiceAdminCommunityDto.Post> changePostBlind(
            @PathVariable Long postId,
            @Valid @RequestBody ServiceAdminCommunityDto.BlindRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminCommunityService.setPostBlind(
                        postId, request.getBlind(), requesterId(authentication))
        );
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Authentication authentication) {
        serviceAdminCommunityService.deletePost(postId, requesterId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members")
    public ResponseEntity<ServiceAdminCommunityDto.MemberPage> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminCommunityService.getMembers(page, keyword, status, requesterId(authentication))
        );
    }

    @PatchMapping("/members/{memberId}/status")
    public ResponseEntity<ServiceAdminCommunityDto.MemberReportSummary> changeMemberStatus(
            @PathVariable Long memberId,
            @Valid @RequestBody ServiceAdminCommunityDto.StatusChangeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminCommunityService.changeMemberStatus(
                        memberId, request.getStatus(), requesterId(authentication))
        );
    }

    private Long requesterId(Authentication authentication) {
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
