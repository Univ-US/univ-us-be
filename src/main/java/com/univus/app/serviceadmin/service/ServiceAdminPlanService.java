package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminPlanDto;

public interface ServiceAdminPlanService {

    ServiceAdminPlanDto.Response getPlans();

    ServiceAdminPlanDto.Plan createPlan(
            ServiceAdminPlanDto.SaveRequest request
    );

    ServiceAdminPlanDto.Plan updatePlan(
            Long planId,
            ServiceAdminPlanDto.SaveRequest request
    );

    ServiceAdminPlanDto.Plan changeStatus(Long planId, String requestedStatus);
}
