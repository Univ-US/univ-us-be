package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminOperationLogDto;

public interface ServiceAdminOperationLogService {

    ServiceAdminOperationLogDto.Page getOperationLogs(
            int page,
            String keyword,
            String category,
            String result,
            String period
    );
}
