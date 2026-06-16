package com.univus.app.serviceadmin.controller;

import com.univus.app.serviceadmin.dto.ServiceAdminPaymentDto;
import com.univus.app.serviceadmin.service.ServiceAdminPaymentService;
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
@RequestMapping("/api/service-admin/payments")
@RequiredArgsConstructor
public class ServiceAdminPaymentController {

    private final ServiceAdminPaymentService serviceAdminPaymentService;

    @GetMapping
    public ResponseEntity<ServiceAdminPaymentDto.Page> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) String method,
            @RequestParam(defaultValue = "RECENT") String sort
    ) {
        return ResponseEntity.ok(serviceAdminPaymentService.getPayments(
                page,
                keyword,
                status,
                planId,
                method,
                sort
        ));
    }

    @PatchMapping("/{historyId}/refund")
    public ResponseEntity<ServiceAdminPaymentDto.Payment> refund(
            @PathVariable Long historyId,
            @Valid @RequestBody ServiceAdminPaymentDto.RefundRequest request
    ) {
        return ResponseEntity.ok(serviceAdminPaymentService.refund(
                historyId,
                request.getReason()
        ));
    }

    @PatchMapping("/{historyId}/retry")
    public ResponseEntity<ServiceAdminPaymentDto.Payment> retry(
            @PathVariable Long historyId
    ) {
        return ResponseEntity.ok(serviceAdminPaymentService.retry(historyId));
    }

    @PatchMapping("/{historyId}/cancel")
    public ResponseEntity<ServiceAdminPaymentDto.Payment> cancelScheduledPayment(
            @PathVariable Long historyId
    ) {
        return ResponseEntity.ok(
                serviceAdminPaymentService.cancelScheduledPayment(historyId)
        );
    }
}
