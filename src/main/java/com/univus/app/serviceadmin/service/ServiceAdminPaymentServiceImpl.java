package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminPaymentDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminPaymentMapper;
import com.univus.app.subscription.service.PortOneBillingClient;
import com.univus.app.subscription.service.PortOnePaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAdminPaymentServiceImpl implements ServiceAdminPaymentService {

    private static final int PAGE_SIZE = 10;
    private static final ZoneId SUBSCRIPTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<String> STATUSES =
            Set.of("READY", "PAID", "FAILED", "CANCELED", "REFUNDED");
    private static final Set<String> METHODS = Set.of("CARD", "KAKAO_PAY");
    private static final Set<String> SORT_OPTIONS =
            Set.of("RECENT", "AMOUNT_DESC", "AMOUNT_ASC", "SCHOOL_ASC");

    private final ServiceAdminPaymentMapper serviceAdminPaymentMapper;
    private final ServiceAdminSchoolService serviceAdminSchoolService;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PortOneBillingClient portOneBillingClient;

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminPaymentDto.Page getPayments(
            int page,
            String keyword,
            String status,
            Long planId,
            String method,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        ServiceAdminPaymentDto.Search search = new ServiceAdminPaymentDto.Search();
        search.setKeyword(normalizeKeyword(keyword));
        search.setStatus(normalizeOption(status, STATUSES, "결제 상태"));
        search.setPlanId(planId);
        search.setMethod(normalizeOption(method, METHODS, "결제 수단"));
        search.setSort(normalizeSort(sort));
        search.setOffset(safePage * PAGE_SIZE);

        return new ServiceAdminPaymentDto.Page(
                serviceAdminPaymentMapper.selectPayments(search),
                safePage,
                PAGE_SIZE,
                serviceAdminPaymentMapper.countPayments(search),
                serviceAdminPaymentMapper.selectPaymentSummary(),
                serviceAdminPaymentMapper.selectPlanOptions()
        );
    }

    @Transactional
    @Override
    public ServiceAdminPaymentDto.Payment refund(Long historyId, String reason) {
        ServiceAdminPaymentDto.OperationTarget target = getTarget(historyId);
        if (!"PAID".equals(target.getStatus())
                || isBlank(target.getPortonePaymentId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "결제 완료 상태의 PortOne 결제만 환불할 수 있습니다."
            );
        }

        PortOnePaymentClient.PortOneCancellation cancellation;
        try {
            cancellation = portOnePaymentClient.cancelPayment(
                    target.getPortonePaymentId(),
                    target.getAmount(),
                    reason.trim()
            );
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "PortOne 결제 환불에 실패했습니다.",
                    exception
            );
        }

        requireSingleRow(
                serviceAdminPaymentMapper.markPaymentRefunded(
                        historyId,
                        toLocalDateTime(cancellation.getCancelledAt()),
                        target.getAmount(),
                        reason.trim(),
                        cancellation.getId()
                ),
                "환불 결과를 결제 이력에 반영하지 못했습니다."
        );
        return getPayment(historyId);
    }

    @Transactional
    @Override
    public ServiceAdminPaymentDto.Payment retry(Long historyId) {
        ServiceAdminPaymentDto.OperationTarget target = getTarget(historyId);
        validateRetryTarget(target);

        ServiceAdminPaymentDto.NewPayment retryPayment =
                ServiceAdminPaymentDto.NewPayment.builder()
                        .subscriptionId(target.getSubscriptionId())
                        .univId(target.getUnivId())
                        .memberId(target.getMemberId())
                        .billingKeyId(target.getBillingKeyId())
                        .merchantUid(createMerchantUid())
                        .amount(target.getAmount())
                        .planName(target.getPlanName())
                        .build();
        requireSingleRow(
                serviceAdminPaymentMapper.insertRetryPayment(retryPayment),
                "재결제 이력을 생성하지 못했습니다."
        );

        PortOnePaymentClient.PortOnePayment payment;
        try {
            portOneBillingClient.payWithBillingKey(
                    retryPayment.getMerchantUid(),
                    target.getPortoneBillingKey(),
                    buildOrderName(target.getPlanName()),
                    target.getAmount(),
                    target.getMemberId()
            );
            payment = portOnePaymentClient.getPayment(retryPayment.getMerchantUid());
            validatePaidPayment(retryPayment.getMerchantUid(), target.getAmount(), payment);
        } catch (RuntimeException exception) {
            requireSingleRow(
                    serviceAdminPaymentMapper.markRetryPaymentFailed(
                            retryPayment.getHistoryId(),
                            "Service administrator retry payment failed."
                    ),
                    "재결제 실패 이력을 반영하지 못했습니다."
            );
            return getPayment(retryPayment.getHistoryId());
        }

        LocalDateTime paidAt = toLocalDateTime(payment.getPaidAt());
        LocalDateTime nextBillingAt =
                calculateNextBillingAt(paidAt, target.getBillingCycle());

        requireSingleRow(
                serviceAdminPaymentMapper.markRetryPaymentPaid(
                        retryPayment.getHistoryId(),
                        payment.getId(),
                        paidAt
                ),
                "재결제 완료 이력을 반영하지 못했습니다."
        );
        requireSingleRow(
                serviceAdminPaymentMapper.updateSubscriptionNextBillingAt(
                        target.getSubscriptionId(),
                        nextBillingAt
                ),
                "다음 결제일을 갱신하지 못했습니다."
        );

        createAndScheduleNextPayment(target, nextBillingAt);
        return getPayment(retryPayment.getHistoryId());
    }

    @Override
    public ServiceAdminPaymentDto.Payment cancelScheduledPayment(Long historyId) {
        ServiceAdminPaymentDto.OperationTarget target = getTarget(historyId);
        if (!"READY".equals(target.getStatus())
                || target.getCurrentReadyHistoryId() == null
                || !historyId.equals(target.getCurrentReadyHistoryId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 예약된 다음 결제만 취소할 수 있습니다."
            );
        }
        serviceAdminSchoolService.scheduleCancellation(target.getUnivId());
        return getPayment(historyId);
    }

    private void createAndScheduleNextPayment(
            ServiceAdminPaymentDto.OperationTarget target,
            LocalDateTime nextBillingAt
    ) {
        ServiceAdminPaymentDto.NewPayment nextPayment =
                ServiceAdminPaymentDto.NewPayment.builder()
                        .subscriptionId(target.getSubscriptionId())
                        .univId(target.getUnivId())
                        .memberId(target.getMemberId())
                        .billingKeyId(target.getBillingKeyId())
                        .merchantUid(createMerchantUid())
                        .amount(target.getAmount())
                        .planName(target.getPlanName())
                        .build();
        requireSingleRow(
                serviceAdminPaymentMapper.insertScheduledPayment(nextPayment),
                "다음 예약 결제 이력을 생성하지 못했습니다."
        );

        Runnable scheduleAction = () -> {
            String scheduleId = null;
            try {
                scheduleId = portOneBillingClient.schedulePayment(
                        nextPayment.getMerchantUid(),
                        target.getPortoneBillingKey(),
                        buildOrderName(target.getPlanName()),
                        target.getAmount(),
                        target.getMemberId(),
                        nextBillingAt.atZone(SUBSCRIPTION_ZONE).toOffsetDateTime()
                );
                requireSingleRow(
                        serviceAdminPaymentMapper.updatePaymentScheduleId(
                                nextPayment.getHistoryId(),
                                scheduleId
                        ),
                        "PortOne 예약 결제 ID를 저장하지 못했습니다."
                );
            } catch (RuntimeException exception) {
                if (scheduleId != null) {
                    revokeScheduleQuietly(scheduleId);
                }
                log.error(
                        "Failed to schedule retry follow-up payment. historyId={}",
                        nextPayment.getHistoryId(),
                        exception
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            scheduleAction.run();
                        }
                    }
            );
        } else {
            scheduleAction.run();
        }
    }

    private ServiceAdminPaymentDto.OperationTarget getTarget(Long historyId) {
        ServiceAdminPaymentDto.OperationTarget target =
                serviceAdminPaymentMapper.selectOperationTarget(historyId);
        if (target == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "결제 이력을 찾을 수 없습니다."
            );
        }
        return target;
    }

    private ServiceAdminPaymentDto.Payment getPayment(Long historyId) {
        ServiceAdminPaymentDto.Payment payment =
                serviceAdminPaymentMapper.selectPaymentById(historyId);
        if (payment == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "결제 이력을 찾을 수 없습니다."
            );
        }
        return payment;
    }

    private void validateRetryTarget(ServiceAdminPaymentDto.OperationTarget target) {
        if (!"FAILED".equals(target.getStatus())
                || isBlank(target.getMerchantUid())
                || !target.getMerchantUid().startsWith("sub_renew_")) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "실패한 정기 결제만 재결제할 수 있습니다."
            );
        }
        if (!"ACTIVE".equals(target.getSubscriptionStatus())
                || "CANCEL".equals(target.getPendingAction())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "활성 구독에서만 재결제할 수 있습니다."
            );
        }
        if (target.getBillingKeyId() == null
                || isBlank(target.getPortoneBillingKey())
                || !"ACTIVE".equals(target.getBillingKeyStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "사용 가능한 정기 결제 수단이 없습니다."
            );
        }
    }

    private void validatePaidPayment(
            String paymentId,
            Long expectedAmount,
            PortOnePaymentClient.PortOnePayment payment
    ) {
        Long paidAmount = payment.getAmount() == null
                ? null : payment.getAmount().getTotal();
        if (!paymentId.equals(payment.getId())
                || !"PAID".equals(payment.getStatus())
                || !expectedAmount.equals(paidAmount)) {
            throw new IllegalStateException("PortOne payment verification failed.");
        }
    }

    private LocalDateTime calculateNextBillingAt(
            LocalDateTime paidAt,
            String billingCycle
    ) {
        return "YEARLY".equalsIgnoreCase(billingCycle)
                ? paidAt.plusYears(1)
                : paidAt.plusMonths(1);
    }

    private LocalDateTime toLocalDateTime(String value) {
        if (isBlank(value)) {
            return LocalDateTime.now(SUBSCRIPTION_ZONE);
        }
        return OffsetDateTime.parse(value)
                .atZoneSameInstant(SUBSCRIPTION_ZONE)
                .toLocalDateTime();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeOption(
            String value,
            Set<String> options,
            String label
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!options.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 " + label + "입니다."
            );
        }
        return normalized;
    }

    private String normalizeSort(String sort) {
        String normalized = sort == null || sort.isBlank()
                ? "RECENT"
                : sort.trim().toUpperCase(Locale.ROOT);
        if (!SORT_OPTIONS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다."
            );
        }
        return normalized;
    }

    private String createMerchantUid() {
        return "sub_renew_"
                + LocalDateTime.now(SUBSCRIPTION_ZONE)
                .format(MERCHANT_UID_TIME_FORMAT)
                + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String buildOrderName(String planName) {
        return "UnivUs " + (isBlank(planName) ? "subscription" : planName);
    }

    private void revokeScheduleQuietly(String scheduleId) {
        try {
            portOneBillingClient.revokePaymentSchedule(scheduleId);
        } catch (RuntimeException revokeException) {
            log.error(
                    "Failed to revoke unrecorded payment schedule. scheduleId={}",
                    scheduleId,
                    revokeException
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }
}
