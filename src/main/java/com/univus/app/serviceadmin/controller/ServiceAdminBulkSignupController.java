package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminBulkSignupDto;
import com.univus.app.serviceadmin.service.ServiceAdminBulkSignupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-admin/bulk-signups")
@RequiredArgsConstructor
public class ServiceAdminBulkSignupController {

    private final ServiceAdminBulkSignupService serviceAdminBulkSignupService;

    @PostMapping("/precheck")
    public ResponseEntity<ServiceAdminBulkSignupDto.PrecheckResponse> precheck(
            @RequestBody ServiceAdminBulkSignupDto.Request request
    ) {
        return ResponseEntity.ok(serviceAdminBulkSignupService.precheck(request));
    }

    @PostMapping
    public ResponseEntity<ServiceAdminBulkSignupDto.RegisterResponse> register(
            @RequestBody ServiceAdminBulkSignupDto.Request request
    ) {
        return ResponseEntity.ok(serviceAdminBulkSignupService.register(request));
    }
}
