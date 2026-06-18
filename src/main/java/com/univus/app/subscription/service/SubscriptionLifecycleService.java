package com.univus.app.subscription.service;

import com.univus.app.subscription.dto.SubscriptionMutationTargetDto;
import com.univus.app.subscription.dto.SubscriptionPaymentHistoryInsertDto;
import com.univus.app.subscription.dto.SubscriptionPlanResponseDto;
import com.univus.app.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// service-admin과 self-service(학교 관리자 본인)가 공통으로 사용하는 구독 플랜 변경/취소 예약 로직입니다.
// 두 흐름 모두 PLAN_ID를 즉시 바꾸지 않고, 다음 결제(webhook 성공) 시점에만 반영되도록 합니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

    private static final String PENDING_ACTION_CANCEL = "CANCEL";
    private static final String PENDING_ACTION_PLAN_CHANGE = "PLAN_CHANGE";
    private static final ZoneId SUBSCRIPTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SubscriptionMapper subscriptionMapper;
    private final PortOneBillingClient portOneBillingClient;
    private final TransactionTemplate transactionTemplate;

    public void changePlan(Long univId, Long planId) {
        SubscriptionMutationTargetDto target = getActiveTarget(univId);
        if (PENDING_ACTION_CANCEL.equals(target.getPendingAction())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "구독 취소 예정 상태에서는 플랜을 변경할 수 없습니다."
            );
        }
        // PLAN_CHANGE가 예약된 상태에서 원래 플랜(target.getPlanId(), 아직 결제로 확정되지 않은
        // 실제 DB 값)을 다시 요청하면, 이는 "변경 예약을 취소해달라"는 의미이지 중복 요청이 아닙니다.
        boolean isRevertingPendingChange = PENDING_ACTION_PLAN_CHANGE.equals(target.getPendingAction())
                && planId.equals(target.getPlanId());
        if (!isRevertingPendingChange && planId.equals(target.getPlanId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 구독 중인 플랜입니다."
            );
        }

        SubscriptionPlanResponseDto plan = subscriptionMapper.findActivePlanById(planId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.");
        }

        validateSchedulingTarget(target);

        String newMerchantUid = createMerchantUid();
        String newScheduleId = null;
        boolean oldScheduleRevoked = false;

        try {
            if (target.getReadyHistoryId() != null && target.getReadyScheduleId() == null) {
                portOneBillingClient.revokePaymentSchedulesByBillingKey(
                        target.getPortoneBillingKey()
                );
                oldScheduleRevoked = true;
            }

            newScheduleId = portOneBillingClient.schedulePayment(
                    newMerchantUid,
                    target.getPortoneBillingKey(),
                    buildOrderName(plan.getPlanName()),
                    plan.getPrice(),
                    target.getMemberId(),
                    target.getNextBillingAt()
                            .atZone(SUBSCRIPTION_ZONE)
                            .toOffsetDateTime()
            );

            if (target.getReadyScheduleId() != null) {
                portOneBillingClient.revokePaymentSchedule(target.getReadyScheduleId());
                oldScheduleRevoked = true;
            }

            String scheduleId = newScheduleId;
            transactionTemplate.executeWithoutResult(status -> {
                if (target.getReadyHistoryId() != null) {
                    requireSingleRow(
                            subscriptionMapper.markPaymentHistoryCanceled(
                                    target.getReadyHistoryId(),
                                    "Subscription plan change requested."
                            ),
                            "예약 결제 내역을 변경하지 못했습니다."
                    );
                }

                if (isRevertingPendingChange) {
                    requireSingleRow(
                            subscriptionMapper.clearPendingPlanChange(target.getSubscriptionId()),
                            "구독 플랜 변경 예약을 취소하지 못했습니다."
                    );
                } else {
                    requireSingleRow(
                            subscriptionMapper.markPendingPlanChange(target.getSubscriptionId()),
                            "구독 플랜 변경을 예약하지 못했습니다."
                    );
                }

                SubscriptionPaymentHistoryInsertDto payment =
                        SubscriptionPaymentHistoryInsertDto.builder()
                                .subscriptionId(target.getSubscriptionId())
                                .univId(target.getUnivId())
                                .memberId(target.getMemberId())
                                .billingKeyId(target.getBillingKeyId())
                                .status("READY")
                                .merchantUid(newMerchantUid)
                                .portoneScheduleId(scheduleId)
                                .amount(plan.getPrice())
                                .planName(plan.getPlanName())
                                .build();
                requireSingleRow(
                        subscriptionMapper.insertPaymentHistory(payment),
                        "변경된 예약 결제 내역을 저장하지 못했습니다."
                );
            });
        } catch (RuntimeException ex) {
            revokeNewScheduleQuietly(newScheduleId);
            if (oldScheduleRevoked) {
                restorePreviousSchedule(target);
            }
            throw toPaymentSyncException(ex);
        }
    }

    public void scheduleCancellation(Long univId) {
        SubscriptionMutationTargetDto target = getActiveTarget(univId);
        if (PENDING_ACTION_CANCEL.equals(target.getPendingAction())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 구독 취소가 예약되어 있습니다."
            );
        }
        if (target.getNextBillingAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "다음 결제일이 없어 구독 종료일을 계산할 수 없습니다."
            );
        }

        boolean oldScheduleRevoked = false;
        try {
            if (target.getReadyHistoryId() != null) {
                validateBillingKey(target);
                if (target.getReadyScheduleId() != null) {
                    portOneBillingClient.revokePaymentSchedule(target.getReadyScheduleId());
                } else {
                    portOneBillingClient.revokePaymentSchedulesByBillingKey(
                            target.getPortoneBillingKey()
                    );
                }
                oldScheduleRevoked = true;
            }

            transactionTemplate.executeWithoutResult(status -> {
                if (target.getReadyHistoryId() != null) {
                    requireSingleRow(
                            subscriptionMapper.markPaymentHistoryCanceled(
                                    target.getReadyHistoryId(),
                                    "Subscription cancellation scheduled."
                            ),
                            "예약 결제 내역을 취소하지 못했습니다."
                    );
                }
                requireSingleRow(
                        subscriptionMapper.scheduleSubscriptionCancellation(
                                target.getSubscriptionId()
                        ),
                        "구독 취소를 예약하지 못했습니다."
                );
            });
        } catch (RuntimeException ex) {
            if (oldScheduleRevoked) {
                restorePreviousSchedule(target);
            }
            throw toPaymentSyncException(ex);
        }
    }

    // 예약된 구독 취소를 철회합니다. scheduleCancellation의 역방향으로,
    // 다음 결제(같은 플랜/금액/날짜)를 다시 예약하고 PENDING_ACTION을 초기화합니다.
    public void resumeSubscription(Long univId) {
        SubscriptionMutationTargetDto target = getActiveTarget(univId);
        if (!PENDING_ACTION_CANCEL.equals(target.getPendingAction())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "예약된 구독 취소가 없습니다."
            );
        }
        validateBillingKey(target);
        if (target.getNextBillingAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "다음 결제일이 없어 구독 취소 예약을 철회할 수 없습니다."
            );
        }

        String merchantUid = createMerchantUid();
        String scheduleId;
        try {
            scheduleId = portOneBillingClient.schedulePayment(
                    merchantUid,
                    target.getPortoneBillingKey(),
                    buildOrderName(target.getPlanName()),
                    target.getPlanPrice(),
                    target.getMemberId(),
                    target.getNextBillingAt()
                            .atZone(SUBSCRIPTION_ZONE)
                            .toOffsetDateTime()
            );
        } catch (RuntimeException ex) {
            throw toPaymentSyncException(ex);
        }

        try {
            transactionTemplate.executeWithoutResult(status -> {
                requireSingleRow(
                        subscriptionMapper.clearPendingCancellation(target.getSubscriptionId()),
                        "구독 취소 예약을 철회하지 못했습니다."
                );

                SubscriptionPaymentHistoryInsertDto payment =
                        SubscriptionPaymentHistoryInsertDto.builder()
                                .subscriptionId(target.getSubscriptionId())
                                .univId(target.getUnivId())
                                .memberId(target.getMemberId())
                                .billingKeyId(target.getBillingKeyId())
                                .status("READY")
                                .merchantUid(merchantUid)
                                .portoneScheduleId(scheduleId)
                                .amount(target.getPlanPrice())
                                .planName(target.getPlanName())
                                .build();
                requireSingleRow(
                        subscriptionMapper.insertPaymentHistory(payment),
                        "다음 결제 예약 내역을 저장하지 못했습니다."
                );
            });
        } catch (RuntimeException ex) {
            revokeNewScheduleQuietly(scheduleId);
            throw toPaymentSyncException(ex);
        }
    }

    private SubscriptionMutationTargetDto getActiveTarget(Long univId) {
        SubscriptionMutationTargetDto target =
                subscriptionMapper.findMutationTargetByUnivId(univId);
        if (target == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "활성 구독을 찾을 수 없습니다."
            );
        }
        return target;
    }

    private void validateSchedulingTarget(SubscriptionMutationTargetDto target) {
        validateBillingKey(target);
        if (target.getNextBillingAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "다음 결제일이 없어 플랜을 변경할 수 없습니다."
            );
        }
    }

    private void validateBillingKey(SubscriptionMutationTargetDto target) {
        if (target.getBillingKeyId() == null
                || target.getPortoneBillingKey() == null
                || target.getPortoneBillingKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "등록된 정기결제 수단이 없습니다."
            );
        }
    }

    private void restorePreviousSchedule(SubscriptionMutationTargetDto target) {
        if (target.getReadyHistoryId() == null
                || target.getReadyAmount() == null
                || target.getNextBillingAt() == null) {
            return;
        }

        try {
            String merchantUid = createMerchantUid();
            String scheduleId = portOneBillingClient.schedulePayment(
                    merchantUid,
                    target.getPortoneBillingKey(),
                    buildOrderName(target.getPlanName()),
                    target.getReadyAmount(),
                    target.getMemberId(),
                    target.getNextBillingAt()
                            .atZone(SUBSCRIPTION_ZONE)
                            .toOffsetDateTime()
            );
            transactionTemplate.executeWithoutResult(status -> requireSingleRow(
                    subscriptionMapper.replaceReadyPaymentSchedule(
                            target.getReadyHistoryId(),
                            merchantUid,
                            scheduleId
                    ),
                    "기존 예약 결제 복구 정보를 저장하지 못했습니다."
            ));
        } catch (RuntimeException restoreException) {
            log.error(
                    "Failed to restore PortOne schedule. subscriptionId={}, historyId={}",
                    target.getSubscriptionId(),
                    target.getReadyHistoryId(),
                    restoreException
            );
        }
    }

    private void revokeNewScheduleQuietly(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return;
        }
        try {
            portOneBillingClient.revokePaymentSchedule(scheduleId);
        } catch (RuntimeException revokeException) {
            log.error("Failed to revoke compensating PortOne schedule. scheduleId={}",
                    scheduleId, revokeException);
        }
    }

    private ResponseStatusException toPaymentSyncException(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException;
        }
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "PortOne 예약 결제와 구독 정보를 동기화하지 못했습니다.",
                exception
        );
    }

    private String buildOrderName(String planName) {
        return "UnivUs " + planName + " subscription";
    }

    private String createMerchantUid() {
        return "sub_renew_"
                + LocalDateTime.now(SUBSCRIPTION_ZONE)
                .format(MERCHANT_UID_TIME_FORMAT)
                + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private void requireSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }
}
