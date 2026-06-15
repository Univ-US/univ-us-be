package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminPaymentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ServiceAdminPaymentMapper {

    List<ServiceAdminPaymentDto.Payment> selectPayments(
            ServiceAdminPaymentDto.Search search
    );

    long countPayments(ServiceAdminPaymentDto.Search search);

    ServiceAdminPaymentDto.Summary selectPaymentSummary();

    List<ServiceAdminPaymentDto.PlanOption> selectPlanOptions();

    ServiceAdminPaymentDto.Payment selectPaymentById(
            @Param("historyId") Long historyId
    );

    ServiceAdminPaymentDto.OperationTarget selectOperationTarget(
            @Param("historyId") Long historyId
    );

    int markPaymentRefunded(
            @Param("historyId") Long historyId,
            @Param("refundedAt") LocalDateTime refundedAt,
            @Param("refundAmount") Long refundAmount,
            @Param("refundReason") String refundReason,
            @Param("portoneCancellationId") String portoneCancellationId
    );

    int insertRetryPayment(ServiceAdminPaymentDto.NewPayment payment);

    int markRetryPaymentPaid(
            @Param("historyId") Long historyId,
            @Param("portonePaymentId") String portonePaymentId,
            @Param("paidAt") LocalDateTime paidAt
    );

    int markRetryPaymentFailed(
            @Param("historyId") Long historyId,
            @Param("failReason") String failReason
    );

    int updateSubscriptionNextBillingAt(
            @Param("subscriptionId") Long subscriptionId,
            @Param("nextBillingAt") LocalDateTime nextBillingAt
    );

    int insertScheduledPayment(ServiceAdminPaymentDto.NewPayment payment);

    int updatePaymentScheduleId(
            @Param("historyId") Long historyId,
            @Param("portoneScheduleId") String portoneScheduleId
    );
}
