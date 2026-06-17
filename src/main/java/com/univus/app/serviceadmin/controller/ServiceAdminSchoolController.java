package com.univus.app.serviceadmin.controller;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.serviceadmin.dto.ServiceAdminSchoolDto;
import com.univus.app.serviceadmin.service.ServiceAdminSchoolService;
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
@RequestMapping("/api/service-admin/schools")
@RequiredArgsConstructor
public class ServiceAdminSchoolController {

    private final ServiceAdminSchoolService serviceAdminSchoolService;

    @GetMapping
    public ResponseEntity<PaginateUtilRestApiRes<ServiceAdminSchoolDto.School>> getSchools(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subscriptionStatus,
            @RequestParam(required = false) Long planId,
            @RequestParam(defaultValue = "NAME_ASC") String sort
    ) {
        return ResponseEntity.ok(serviceAdminSchoolService.getSchools(
                page,
                keyword,
                subscriptionStatus,
                planId,
                sort
        ));
    }

    @GetMapping("/{univId}")
    public ResponseEntity<ServiceAdminSchoolDto.School> getSchool(
            @PathVariable Long univId
    ) {
        return ResponseEntity.ok(serviceAdminSchoolService.getSchool(univId));
    }

    @PatchMapping("/{univId}/plan")
    public ResponseEntity<ServiceAdminSchoolDto.School> changePlan(
            @PathVariable Long univId,
            @Valid @RequestBody ServiceAdminSchoolDto.PlanChangeRequest request
    ) {
        return ResponseEntity.ok(
                serviceAdminSchoolService.changePlan(univId, request.getPlanId())
        );
    }

    @PatchMapping("/{univId}/cancel")
    public ResponseEntity<ServiceAdminSchoolDto.School> cancelSubscription(
            @PathVariable Long univId
    ) {
        return ResponseEntity.ok(
                serviceAdminSchoolService.scheduleCancellation(univId)
        );
    }
}
