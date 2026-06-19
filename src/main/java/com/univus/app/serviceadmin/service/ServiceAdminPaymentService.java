package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminPaymentDto;

public interface ServiceAdminPaymentService {

    ServiceAdminPaymentDto.Page getPayments(
            int page,
            String keyword,
            String status,
            Long planId,
            String method,
            String sort
    );

    ServiceAdminPaymentDto.Payment refund(Long historyId, String reason);

    ServiceAdminPaymentDto.Payment retry(Long historyId);

    ServiceAdminPaymentDto.Payment cancelScheduledPayment(Long historyId);
}
