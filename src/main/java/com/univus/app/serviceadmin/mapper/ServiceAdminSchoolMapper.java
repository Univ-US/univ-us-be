package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminSchoolDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceAdminSchoolMapper {

    List<ServiceAdminSchoolDto.School> selectSchools(ServiceAdminSchoolDto.Search search);

    long countSchools(ServiceAdminSchoolDto.Search search);

    ServiceAdminSchoolDto.School selectSchoolById(@Param("univId") Long univId);

    List<ServiceAdminSchoolDto.Admin> selectAdminsByUniversityIds(
            @Param("univIds") List<Long> univIds
    );

    List<ServiceAdminSchoolDto.Admin> selectAdminsByUniversityId(
            @Param("univId") Long univId
    );

    ServiceAdminSchoolDto.MutationTarget selectMutationTarget(
            @Param("univId") Long univId
    );

    ServiceAdminSchoolDto.Plan selectPlan(@Param("planId") Long planId);

    int updateSubscriptionPlan(
            @Param("subscriptionId") Long subscriptionId,
            @Param("planId") Long planId
    );

    int scheduleSubscriptionCancellation(@Param("subscriptionId") Long subscriptionId);

    int markReadyPaymentCanceled(
            @Param("historyId") Long historyId,
            @Param("reason") String reason
    );

    int insertScheduledPayment(ServiceAdminSchoolDto.ScheduledPayment payment);

    int replaceReadyPaymentSchedule(
            @Param("historyId") Long historyId,
            @Param("merchantUid") String merchantUid,
            @Param("portoneScheduleId") String portoneScheduleId
    );

    int finalizeScheduledCancellations();
}
