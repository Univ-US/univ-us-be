package com.univus.app.subscription.service;

import com.univus.app.security.JwtTokenProvider;
import com.univus.app.subscription.dto.SubscriptionBillingKeyInsertDto;
import com.univus.app.subscription.dto.SubscriptionBillingPaymentRequestDto;
import com.univus.app.subscription.dto.SubscriptionPaymentHistoryInsertDto;
import com.univus.app.subscription.dto.SubscriptionPaymentVerifyResponseDto;
import com.univus.app.subscription.dto.SubscriptionPaymentVerifyTargetDto;
import com.univus.app.subscription.dto.SubscriptionRenewalTargetDto;
import com.univus.app.subscription.dto.SubscriptionUniversityDto;
import com.univus.app.subscription.dto.SubscriptionWebhookRequestDto;
import com.univus.app.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionBillingServiceImpl implements SubscriptionBillingService {

    private static final String PAYMENT_STATUS_READY = "READY";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_FAILED = "FAILED";
    private static final String SUBSCRIPTION_STATUS_PENDING = "PENDING";
    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String APPLICATION_STATUS_PENDING = "PENDING";
    private static final String BILLING_KEY_STATUS_ACTIVE = "ACTIVE";
    private static final String PORTONE_BILLING_KEY_STATUS_ISSUED = "ISSUED";
    private static final String MEMBER_ROLE_ADMIN = "ADM";
    private static final ZoneId SUBSCRIPTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SubscriptionMapper subscriptionMapper;
    private final PortOneBillingClient portOneBillingClient;
    private final PortOnePaymentClient portOnePaymentClient;
    private final SubscriptionPaymentFailureRecorder failureRecorder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${portone.subscription.store-id:}")
    private String portOneStoreId;

    @Override
    @Transactional
    public SubscriptionPaymentVerifyResponseDto completeInitialBillingPayment(
            Long memberId,
            SubscriptionBillingPaymentRequestDto request
    ) {
        validateInitialRequest(memberId, request);

        SubscriptionPaymentVerifyTargetDto target =
                subscriptionMapper.findPaymentVerifyTargetByMerchantUid(request.getMerchantUid());

        validateInitialTarget(memberId, target);

        PortOneBillingClient.PortOneBillingKey portOneBillingKey =
                portOneBillingClient.getBillingKey(request.getBillingKey());

        if (!request.getBillingKey().equals(portOneBillingKey.getBillingKey())
                || !PORTONE_BILLING_KEY_STATUS_ISSUED.equals(portOneBillingKey.getStatus())) {
            throw new IllegalStateException("The issued billing key could not be verified.");
        }

        SubscriptionBillingKeyInsertDto billingKey = SubscriptionBillingKeyInsertDto.builder()
                .memberId(memberId)
                .billingKeyValue(request.getBillingKey())
                .pgProvider(toPgProvider(request.getPaymentMethod()))
                .status(BILLING_KEY_STATUS_ACTIVE)
                .build();

        PortOnePaymentClient.PortOnePayment payment;
        try {
            portOneBillingClient.payWithBillingKey(
                    target.getMerchantUid(),
                    request.getBillingKey(),
                    buildOrderName(target.getPlanName()),
                    target.getAmount(),
                    memberId
            );
            payment = portOnePaymentClient.getPayment(target.getMerchantUid());
            validatePaidPayment(target.getMerchantUid(), target.getAmount(), payment);
        } catch (RuntimeException ex) {
            markInitialPaymentFailed(target, "PortOne billing payment request failed.");
            throw new IllegalStateException("The initial recurring payment failed.", ex);
        }

        LocalDateTime paidAt = toLocalDateTime(payment.getPaidAt());
        LocalDateTime nextBillingAt = calculateNextBillingAt(paidAt, target.getBillingCycle());
        Long univId = resolveUniversityId(target);
        billingKey.setUnivId(univId);

        Long existingBillingKeyId =
                subscriptionMapper.findBillingKeyIdByMemberId(memberId);
        if (existingBillingKeyId == null) {
            requireSingleRow(
                    subscriptionMapper.insertBillingKey(billingKey),
                    "Failed to save the billing key."
            );
        } else {
            billingKey.setBillingKeyId(existingBillingKeyId);
            requireSingleRow(
                    subscriptionMapper.updateBillingKeyForResubscription(billingKey),
                    "Failed to update the billing key."
            );
        }
        requireSingleRow(
                subscriptionMapper.attachSubscriptionBillingKey(
                        target.getSubscriptionId(),
                        billingKey.getBillingKeyId()
                ),
                "Failed to connect the billing key to the subscription."
        );
        requireSingleRow(
                subscriptionMapper.attachPaymentHistoryBillingKey(
                        target.getPaymentHistoryId(),
                        billingKey.getBillingKeyId()
                ),
                "Failed to connect the billing key to the payment."
        );

        requireSingleRow(
                subscriptionMapper.markPaymentHistoryPaid(
                        target.getPaymentHistoryId(),
                        payment.getId(),
                        paidAt,
                        univId
                ),
                "Failed to complete the initial payment history."
        );
        requireSingleRow(
                subscriptionMapper.activateSubscription(
                        target.getSubscriptionId(),
                        univId,
                        nextBillingAt
                ),
                "Failed to activate the subscription."
        );
        requireSingleRow(
                subscriptionMapper.updateMemberAsUniversityAdmin(memberId, univId),
                "Failed to grant the university administrator role."
        );
        if (target.getApplicationId() != null) {
            requireSingleRow(
                    subscriptionMapper.completeSubscriptionApplication(
                            target.getApplicationId()
                    ),
                    "Failed to complete the subscription application."
            );
        }

        createAndScheduleNextPayment(
                target.getSubscriptionId(),
                billingKey.getBillingKeyId(),
                memberId,
                univId,
                request.getBillingKey(),
                target.getPlanName(),
                target.getAmount(),
                nextBillingAt
        );

        String accessToken = jwtTokenProvider.createAccessToken(memberId, MEMBER_ROLE_ADMIN);

        return SubscriptionPaymentVerifyResponseDto.builder()
                .subscriptionId(target.getSubscriptionId())
                .paymentHistoryId(target.getPaymentHistoryId())
                .portonePaymentId(payment.getId())
                .merchantUid(target.getMerchantUid())
                .amount(target.getAmount())
                .paymentStatus(PAYMENT_STATUS_PAID)
                .subscriptionStatus(SUBSCRIPTION_STATUS_ACTIVE)
                .paidAt(paidAt)
                .nextBillingAt(nextBillingAt)
                .memberId(memberId)
                .univId(univId)
                .role(MEMBER_ROLE_ADMIN)
                .accessToken(accessToken)
                .tokenType(jwtTokenProvider.getTokenType())
                .build();
    }

    @Override
    @Transactional
    public void handleWebhook(SubscriptionWebhookRequestDto request) {
        if (request == null || request.getData() == null) {
            return;
        }
        if (!portOneStoreId.equals(request.getData().getStoreId())) {
            throw new IllegalArgumentException("Invalid PortOne store.");
        }
        if (isBlank(request.getData().getPaymentId())) {
            return;
        }

        String eventType = request.getType();
        if (!"Transaction.Paid".equals(eventType)
                && !"Transaction.Failed".equals(eventType)) {
            return;
        }

        SubscriptionRenewalTargetDto target =
                subscriptionMapper.findRenewalTargetByMerchantUid(
                        request.getData().getPaymentId()
                );

        if (target == null || !PAYMENT_STATUS_READY.equals(target.getPaymentStatus())) {
            return;
        }

        PortOnePaymentClient.PortOnePayment payment =
                portOnePaymentClient.getPayment(target.getMerchantUid());

        if ("Transaction.Failed".equals(eventType)) {
            if (!PAYMENT_STATUS_FAILED.equals(payment.getStatus())) {
                return;
            }

            requireSingleRow(
                    subscriptionMapper.markPaymentHistoryFailed(
                            target.getPaymentHistoryId(),
                            "PortOne recurring payment failed."
                    ),
                    "Failed to save the recurring payment failure."
            );
            return;
        }

        validatePaidPayment(target.getMerchantUid(), target.getAmount(), payment);

        LocalDateTime paidAt = toLocalDateTime(payment.getPaidAt());
        LocalDateTime nextBillingAt =
                calculateNextBillingAt(paidAt, target.getBillingCycle());

        requireSingleRow(
                subscriptionMapper.markRenewalPaymentPaid(
                        target.getPaymentHistoryId(),
                        payment.getId(),
                        paidAt
                ),
                "Failed to complete the recurring payment history."
        );
        requireSingleRow(
                subscriptionMapper.updateNextBillingAt(
                        target.getSubscriptionId(),
                        nextBillingAt,
                        target.getPlanName()
                ),
                "Failed to update the next billing date."
        );

        createAndScheduleNextPayment(
                target.getSubscriptionId(),
                target.getBillingKeyId(),
                target.getMemberId(),
                target.getUnivId(),
                target.getPortoneBillingKey(),
                target.getPlanName(),
                target.getAmount(),
                nextBillingAt
        );
    }

    private void createAndScheduleNextPayment(
            Long subscriptionId,
            Long billingKeyId,
            Long memberId,
            Long univId,
            String portoneBillingKey,
            String planName,
            Long amount,
            LocalDateTime nextBillingAt
    ) {
        SubscriptionPaymentHistoryInsertDto nextPayment =
                SubscriptionPaymentHistoryInsertDto.builder()
                        .subscriptionId(subscriptionId)
                        .univId(univId)
                        .memberId(memberId)
                        .billingKeyId(billingKeyId)
                        .status(PAYMENT_STATUS_READY)
                        .merchantUid(createMerchantUid())
                        .amount(amount)
                        .planName(planName)
                        .build();

        requireSingleRow(
                subscriptionMapper.insertPaymentHistory(nextPayment),
                "Failed to create the next recurring payment history."
        );

        Runnable scheduleAction = () -> {
            String scheduleId = null;
            try {
                scheduleId = portOneBillingClient.schedulePayment(
                        nextPayment.getMerchantUid(),
                        portoneBillingKey,
                        buildOrderName(planName),
                        amount,
                        memberId,
                        nextBillingAt.atZone(SUBSCRIPTION_ZONE).toOffsetDateTime()
                );
                requireSingleRow(
                        subscriptionMapper.updatePaymentScheduleId(
                                nextPayment.getHistoryId(),
                                scheduleId
                        ),
                        "Failed to save the PortOne payment schedule ID."
                );
            } catch (RuntimeException ex) {
                if (scheduleId != null) {
                    try {
                        portOneBillingClient.revokePaymentSchedule(scheduleId);
                    } catch (RuntimeException revokeException) {
                        log.error(
                                "Failed to revoke an unrecorded subscription schedule. scheduleId={}",
                                scheduleId,
                                revokeException
                        );
                    }
                }
                log.error(
                        "Failed to schedule subscription payment. merchantUid={}",
                        nextPayment.getMerchantUid(),
                        ex
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

    private Long resolveUniversityId(SubscriptionPaymentVerifyTargetDto target) {
        if (target.getApplicationId() == null && target.getUnivId() != null) {
            return target.getUnivId();
        }

        if (target.getApplicationId() == null
                || !APPLICATION_STATUS_PENDING.equals(target.getApplicationStatus())) {
            throw new IllegalStateException("The pending subscription application was not found.");
        }
        if (isBlank(target.getUnivName())
                || isBlank(target.getSido())
                || isBlank(target.getAddress())
                || isBlank(target.getSchoolPhone())
                || isBlank(target.getHomepage())) {
            throw new IllegalStateException("The university application is incomplete.");
        }

        SubscriptionUniversityDto university = SubscriptionUniversityDto.builder()
                .univName(target.getUnivName())
                .sido(target.getSido())
                .address(target.getAddress())
                .schoolPhone(target.getSchoolPhone())
                .homepage(target.getHomepage())
                .build();

        requireSingleRow(
                subscriptionMapper.insertUniversity(university),
                "Failed to create the university."
        );
        return university.getUnivId();
    }

    private void validateInitialRequest(
            Long memberId,
            SubscriptionBillingPaymentRequestDto request
    ) {
        if (memberId == null) {
            throw new IllegalArgumentException("Login is required.");
        }
        if (request == null
                || isBlank(request.getMerchantUid())
                || isBlank(request.getBillingKey())
                || isBlank(request.getPaymentMethod())) {
            throw new IllegalArgumentException("Billing payment information is required.");
        }
        normalizePaymentMethod(request.getPaymentMethod());
    }

    private void validateInitialTarget(
            Long memberId,
            SubscriptionPaymentVerifyTargetDto target
    ) {
        if (target == null) {
            throw new IllegalArgumentException("The prepared payment was not found.");
        }
        if (!memberId.equals(target.getMemberId())) {
            throw new IllegalArgumentException("The payment owner does not match.");
        }
        boolean pendingApplication =
                target.getApplicationId() != null
                        && APPLICATION_STATUS_PENDING.equals(
                                target.getApplicationStatus()
                        );
        boolean existingUniversity =
                target.getApplicationId() == null && target.getUnivId() != null;

        if (!PAYMENT_STATUS_READY.equals(target.getPaymentStatus())
                || !SUBSCRIPTION_STATUS_PENDING.equals(target.getSubscriptionStatus())
                || (!pendingApplication && !existingUniversity)) {
            throw new IllegalStateException("The subscription is not ready for billing.");
        }
    }

    private void validatePaidPayment(
            String paymentId,
            Long expectedAmount,
            PortOnePaymentClient.PortOnePayment payment
    ) {
        Long paidAmount = payment.getAmount() == null
                ? null
                : payment.getAmount().getTotal();

        if (!paymentId.equals(payment.getId())
                || !PAYMENT_STATUS_PAID.equals(payment.getStatus())
                || !expectedAmount.equals(paidAmount)) {
            throw new IllegalStateException("The PortOne payment verification failed.");
        }
    }

    private void markInitialPaymentFailed(
            SubscriptionPaymentVerifyTargetDto target,
            String reason
    ) {
        failureRecorder.markFailed(
                target.getPaymentHistoryId(),
                target.getSubscriptionId(),
                target.getApplicationId(),
                reason
        );
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!"CARD".equals(normalized) && !"KAKAO_PAY".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported recurring payment method.");
        }
        return normalized;
    }

    private String toPgProvider(String paymentMethod) {
        return "CARD".equals(normalizePaymentMethod(paymentMethod))
                ? "tosspayments"
                : "kakaopay";
    }

    private LocalDateTime calculateNextBillingAt(
            LocalDateTime paidAt,
            String billingCycle
    ) {
        if ("YEARLY".equalsIgnoreCase(billingCycle)) {
            return paidAt.plusYears(1);
        }
        return paidAt.plusMonths(1);
    }

    private String buildOrderName(String planName) {
        return "UnivUs " + (isBlank(planName) ? "subscription" : planName);
    }

    private String createMerchantUid() {
        String timestamp = LocalDateTime.now(SUBSCRIPTION_ZONE)
                .format(MERCHANT_UID_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8);
        return "sub_renew_" + timestamp + "_" + suffix;
    }

    private LocalDateTime toLocalDateTime(String paidAt) {
        if (isBlank(paidAt)) {
            return LocalDateTime.now(SUBSCRIPTION_ZONE);
        }
        return OffsetDateTime.parse(paidAt)
                .atZoneSameInstant(SUBSCRIPTION_ZONE)
                .toLocalDateTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
