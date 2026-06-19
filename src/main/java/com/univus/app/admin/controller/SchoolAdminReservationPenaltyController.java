package com.univus.app.admin.controller;

import com.univus.app.admin.dto.SchoolAdminReservationPenaltyDto;
import com.univus.app.admin.service.SchoolAdminReservationPenaltyService;
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
public class SchoolAdminReservationPenaltyController {

    private final SchoolAdminReservationPenaltyService schoolAdminReservationPenaltyService;

    @GetMapping
    public ResponseEntity<SchoolAdminReservationPenaltyDto.PenaltyPage> getPenalties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminReservationPenaltyService.getPenalties(
                        page, memberId, keyword, status, requesterId(authentication))
        );
    }

    @GetMapping("/members/{memberId}/status")
    public ResponseEntity<SchoolAdminReservationPenaltyDto.MemberPenaltyStatus> getMemberStatus(
            @PathVariable Long memberId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminReservationPenaltyService.getMemberStatus(memberId, requesterId(authentication))
        );
    }

    @PostMapping
    public ResponseEntity<SchoolAdminReservationPenaltyDto.Penalty> grantPenalty(
            @Valid @RequestBody SchoolAdminReservationPenaltyDto.GrantRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminReservationPenaltyService.grantPenalty(
                        request.getMemberId(), request.getReason(), requesterId(authentication))
        );
    }

    @PatchMapping("/{penaltyId}/release")
    public ResponseEntity<SchoolAdminReservationPenaltyDto.Penalty> releasePenalty(
            @PathVariable Long penaltyId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                schoolAdminReservationPenaltyService.releasePenalty(penaltyId, requesterId(authentication))
        );
    }

    private Long requesterId(Authentication authentication) {
        return Long.valueOf(authentication.getPrincipal().toString());
    }
}
