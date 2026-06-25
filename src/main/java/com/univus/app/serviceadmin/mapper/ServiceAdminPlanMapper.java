package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminPlanDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceAdminPlanMapper {

    List<ServiceAdminPlanDto.Plan> selectPlans();

    ServiceAdminPlanDto.Plan selectPlan(@Param("planId") Long planId);

    Long selectCurrentMonthRevenue();

    int countDuplicateName(
            @Param("planName") String planName,
            @Param("excludePlanId") Long excludePlanId
    );

    int insertPlan(ServiceAdminPlanDto.SaveRequest request);

    int updatePlan(
            @Param("planId") Long planId,
            @Param("request") ServiceAdminPlanDto.SaveRequest request
    );

    int deactivatePlan(@Param("planId") Long planId);

    int activatePlan(@Param("planId") Long planId);

    List<ServiceAdminPlanDto.ReadyPaymentTarget> selectReadyPayments(
            @Param("planName") String planName
    );

    int replaceReadyPaymentSchedule(
            @Param("historyId") Long historyId,
            @Param("merchantUid") String merchantUid,
            @Param("portoneScheduleId") String portoneScheduleId,
            @Param("amount") Long amount,
            @Param("planName") String planName
    );
}
