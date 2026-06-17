package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminDashboardDto;
import com.univus.app.serviceadmin.service.ServiceAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-admin")
@RequiredArgsConstructor
public class ServiceAdminDashboardController {

    private final ServiceAdminDashboardService serviceAdminDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<ServiceAdminDashboardDto.Response> getDashboard() {
        return ResponseEntity.ok(serviceAdminDashboardService.getDashboard());
    }
}
