package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminBulkSignupDto;

public interface ServiceAdminBulkSignupService {

    ServiceAdminBulkSignupDto.PrecheckResponse precheck(
            ServiceAdminBulkSignupDto.Request request
    );

    ServiceAdminBulkSignupDto.RegisterResponse register(
            ServiceAdminBulkSignupDto.Request request
    );
}
