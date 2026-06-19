package com.univus.app.serviceadmin.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.serviceadmin.dto.ServiceAdminSchoolDto;

public interface ServiceAdminSchoolService {

    PaginateUtilRestApiRes<ServiceAdminSchoolDto.School> getSchools(
            int page,
            String keyword,
            String subscriptionStatus,
            Long planId,
            String sort
    );

    ServiceAdminSchoolDto.School getSchool(Long univId);

    ServiceAdminSchoolDto.School changePlan(Long univId, Long planId);

    ServiceAdminSchoolDto.School scheduleCancellation(Long univId);
}
