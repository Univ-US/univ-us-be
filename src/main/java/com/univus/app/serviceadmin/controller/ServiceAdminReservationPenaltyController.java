package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminReservationPenaltyDto;
import com.univus.app.serviceadmin.service.ServiceAdminReservationPenaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-admin/reservations/penalties")
@RequiredArgsConstructor
public class ServiceAdminReservationPenaltyController {

    private final ServiceAdminReservationPenaltyService serviceAdminReservationPenaltyService;

    @GetMapping
    public ResponseEntity<ServiceAdminReservationPenaltyDto.PenaltyPage> getPenalties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminReservationPenaltyService.getPenalties(
                        page, memberId, keyword, status, requesterId(authentication))
        );
    }

    @GetMapping("/members/{memberId}/status")
    public ResponseEntity<ServiceAdminReservationPenaltyDto.MemberPenaltyStatus> getMemberStatus(
            @PathVariable Long memberId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminReservationPenaltyService.getMemberStatus(memberId, requesterId(authentication))
        );
    }

    @PostMapping
    public ResponseEntity<ServiceAdminReservationPenaltyDto.Penalty> grantPenalty(
            @Valid @RequestBody ServiceAdminReservationPenaltyDto.GrantRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminReservationPenaltyService.grantPenalty(
                        request.getMemberId(), request.getReason(), requesterId(authentication))
        );
    }

    @PatchMapping("/{penaltyId}/release")
    public ResponseEntity<ServiceAdminReservationPenaltyDto.Penalty> releasePenalty(
            @PathVariable Long penaltyId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                serviceAdminReservationPenaltyService.releasePenalty(penaltyId, requesterId(authentication))
        );
    }

    private Long requesterId(Authentication authentication) {
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
