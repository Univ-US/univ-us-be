package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminMemberDto;
import com.univus.app.serviceadmin.service.ServiceAdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-admin/members")
@RequiredArgsConstructor
public class ServiceAdminMemberController {

    private final ServiceAdminMemberService serviceAdminMemberService;

    @GetMapping
    public ResponseEntity<ServiceAdminMemberDto.Page> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "SCHOOL_ASC") String sort
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getMembers(
                        page,
                        keyword,
                        status,
                        sort
                )
        );
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ServiceAdminMemberDto.MemberDetail> getMemberDetail(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getMemberDetail(memberId)
        );
    }

    @GetMapping("/{memberId}/login-logs")
    public ResponseEntity<ServiceAdminMemberDto.UserLoginLogPage> getMemberLoginLogs(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getMemberLoginLogs(memberId, page, size)
        );
    }

    @GetMapping("/{memberId}/activities/{activityType}")
    public ResponseEntity<ServiceAdminMemberDto.UserActivityItemPage> getMemberActivities(
            @PathVariable Long memberId,
            @PathVariable String activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getMemberActivities(
                        memberId,
                        activityType,
                        page,
                        size
                )
        );
    }

    @PatchMapping("/{memberId}/status")
    public ResponseEntity<ServiceAdminMemberDto.Member> changeStatus(
            @PathVariable Long memberId,
            @Valid @RequestBody ServiceAdminMemberDto.StatusChangeRequest request
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.changeStatus(
                        memberId,
                        request.getStatus()
                )
        );
    }

    @PostMapping("/{memberId}/force-logout")
    public ResponseEntity<ServiceAdminMemberDto.ForceLogoutResponse> forceLogoutMember(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.forceLogoutMember(memberId)
        );
    }

    @GetMapping("/users")
    public ResponseEntity<ServiceAdminMemberDto.UserPage> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long univId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "JOINED_DESC") String sort
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getUsers(
                        page,
                        keyword,
                        univId,
                        role,
                        status,
                        sort
                )
        );
    }

    @GetMapping("/users/{memberId}")
    public ResponseEntity<ServiceAdminMemberDto.UserDetail> getUserDetail(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getUserDetail(memberId)
        );
    }

    @GetMapping("/users/{memberId}/login-logs")
    public ResponseEntity<ServiceAdminMemberDto.UserLoginLogPage> getUserLoginLogs(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getUserLoginLogs(memberId, page, size)
        );
    }

    @GetMapping("/users/{memberId}/activities/{activityType}")
    public ResponseEntity<ServiceAdminMemberDto.UserActivityItemPage> getUserActivities(
            @PathVariable Long memberId,
            @PathVariable String activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.getUserActivities(
                        memberId,
                        activityType,
                        page,
                        size
                )
        );
    }

    @PatchMapping("/users/{memberId}/status")
    public ResponseEntity<ServiceAdminMemberDto.User> changeUserStatus(
            @PathVariable Long memberId,
            @Valid @RequestBody ServiceAdminMemberDto.StatusChangeRequest request
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.changeUserStatus(
                        memberId,
                        request.getStatus()
                )
        );
    }

    @PostMapping("/users/{memberId}/force-logout")
    public ResponseEntity<ServiceAdminMemberDto.ForceLogoutResponse> forceLogoutUser(
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(
                serviceAdminMemberService.forceLogoutUser(memberId)
        );
    }
}
