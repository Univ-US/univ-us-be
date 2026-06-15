package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminMemberDto;
import com.univus.app.serviceadmin.service.ServiceAdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
