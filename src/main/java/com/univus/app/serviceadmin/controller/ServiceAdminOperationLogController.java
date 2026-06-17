package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminOperationLogDto;
import com.univus.app.serviceadmin.service.ServiceAdminOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-admin/operation-logs")
@RequiredArgsConstructor
public class ServiceAdminOperationLogController {

    private final ServiceAdminOperationLogService serviceAdminOperationLogService;

    @GetMapping
    public ResponseEntity<ServiceAdminOperationLogDto.Page> getOperationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "30") String period
    ) {
        return ResponseEntity.ok(serviceAdminOperationLogService.getOperationLogs(
                page,
                keyword,
                category,
                result,
                period
        ));
    }
}
