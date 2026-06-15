package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminPlanDto;
import com.univus.app.serviceadmin.service.ServiceAdminPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-admin/plans")
@RequiredArgsConstructor
public class ServiceAdminPlanController {

    private final ServiceAdminPlanService serviceAdminPlanService;

    @GetMapping
    public ResponseEntity<ServiceAdminPlanDto.Response> getPlans() {
        return ResponseEntity.ok(serviceAdminPlanService.getPlans());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceAdminPlanDto.Plan createPlan(
            @Valid @RequestBody ServiceAdminPlanDto.SaveRequest request
    ) {
        return serviceAdminPlanService.createPlan(request);
    }

    @PatchMapping("/{planId}")
    public ServiceAdminPlanDto.Plan updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody ServiceAdminPlanDto.SaveRequest request
    ) {
        return serviceAdminPlanService.updatePlan(planId, request);
    }

    @PatchMapping("/{planId}/status")
    public ServiceAdminPlanDto.Plan changeStatus(
            @PathVariable Long planId,
            @Valid @RequestBody ServiceAdminPlanDto.StatusRequest request
    ) {
        return serviceAdminPlanService.changeStatus(planId, request.getStatus());
    }
}
