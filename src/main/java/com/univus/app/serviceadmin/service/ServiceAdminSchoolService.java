package com.univus.app.serviceadmin.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.serviceadmin.dto.ServiceAdminSchoolDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminSchoolMapper;
import com.univus.app.subscription.service.PortOneBillingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAdminSchoolService {

    private static final int PAGE_SIZE = 10;
    private static final ZoneId SUBSCRIPTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<String> SUBSCRIPTION_STATUSES = Set.of(
            "ACTIVE", "PAST_DUE", "PENDING", "CANCELED", "UNSUBSCRIBED"
    );
    private static final Set<String> SORT_OPTIONS = Set.of(
            "NAME_ASC", "MEMBERS_ASC", "MEMBERS_DESC", "REVENUE_ASC", "REVENUE_DESC"
    );

    private final ServiceAdminSchoolMapper serviceAdminSchoolMapper;
    private final PortOneBillingClient portOneBillingClient;
    private final TransactionTemplate transactionTemplate;

    public PaginateUtilRestApiRes<ServiceAdminSchoolDto.School> getSchools(
            int page,
            String keyword,
            String subscriptionStatus,
            Long planId,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        ServiceAdminSchoolDto.Search search = new ServiceAdminSchoolDto.Search();
        search.setKeyword(normalizeKeyword(keyword));
        search.setSubscriptionStatus(normalizeSubscriptionStatus(subscriptionStatus));
        search.setPlanId(planId);
        search.setSort(normalizeSort(sort));
        search.setOffset(safePage * PAGE_SIZE);

        List<ServiceAdminSchoolDto.School> schools =
                serviceAdminSchoolMapper.selectSchools(search);
        attachAdmins(schools);

        return new PaginateUtilRestApiRes<>(
                schools,
                safePage,
                PAGE_SIZE,
                serviceAdminSchoolMapper.countSchools(search)
        );
    }

    public ServiceAdminSchoolDto.School getSchool(Long univId) {
        ServiceAdminSchoolDto.School school =
                serviceAdminSchoolMapper.selectSchoolById(univId);
        if (school == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학교를 찾을 수 없습니다.");
        }
        school.setAdmins(serviceAdminSchoolMapper.selectAdminsByUniversityId(univId));
        return school;
    }

    public ServiceAdminSchoolDto.School changePlan(Long univId, Long planId) {
        ServiceAdminSchoolDto.MutationTarget target = getActiveTarget(univId);
        if ("CANCEL".equals(target.getPendingAction())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "구독 취소 예정 상태에서는 플랜을 변경할 수 없습니다."
            );
        }

        ServiceAdminSchoolDto.Plan plan = serviceAdminSchoolMapper.selectPlan(planId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다.");
        }
        if (planId.equals(target.getPlanId())) {
            return getSchool(univId);
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
                            serviceAdminSchoolMapper.markReadyPaymentCanceled(
                                    target.getReadyHistoryId(),
                                    "Service administrator changed the subscription plan."
                            ),
                            "예약 결제 내역을 변경하지 못했습니다."
                    );
                }

                requireSingleRow(
                        serviceAdminSchoolMapper.updateSubscriptionPlan(
                                target.getSubscriptionId(),
                                planId
                        ),
                        "구독 플랜을 변경하지 못했습니다."
                );

                ServiceAdminSchoolDto.ScheduledPayment payment =
                        ServiceAdminSchoolDto.ScheduledPayment.builder()
                                .subscriptionId(target.getSubscriptionId())
                                .univId(target.getUnivId())
                                .memberId(target.getMemberId())
                                .billingKeyId(target.getBillingKeyId())
                                .merchantUid(newMerchantUid)
                                .portoneScheduleId(scheduleId)
                                .amount(plan.getPrice())
                                .build();
                requireSingleRow(
                        serviceAdminSchoolMapper.insertScheduledPayment(payment),
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

        return getSchool(univId);
    }

    public ServiceAdminSchoolDto.School scheduleCancellation(Long univId) {
        ServiceAdminSchoolDto.MutationTarget target = getActiveTarget(univId);
        if ("CANCEL".equals(target.getPendingAction())) {
            return getSchool(univId);
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
                            serviceAdminSchoolMapper.markReadyPaymentCanceled(
                                    target.getReadyHistoryId(),
                                    "Subscription cancellation scheduled by service administrator."
                            ),
                            "예약 결제 내역을 취소하지 못했습니다."
                    );
                }
                requireSingleRow(
                        serviceAdminSchoolMapper.scheduleSubscriptionCancellation(
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

        return getSchool(univId);
    }

    private void attachAdmins(List<ServiceAdminSchoolDto.School> schools) {
        if (schools.isEmpty()) {
            return;
        }

        List<Long> univIds = schools.stream()
                .map(ServiceAdminSchoolDto.School::getUnivId)
                .toList();
        Map<Long, List<ServiceAdminSchoolDto.Admin>> adminsByUniversity =
                serviceAdminSchoolMapper.selectAdminsByUniversityIds(univIds)
                        .stream()
                        .collect(Collectors.groupingBy(ServiceAdminSchoolDto.Admin::getUnivId));

        schools.forEach(school -> school.setAdmins(
                adminsByUniversity.getOrDefault(school.getUnivId(), List.of())
        ));
    }

    private ServiceAdminSchoolDto.MutationTarget getActiveTarget(Long univId) {
        ServiceAdminSchoolDto.MutationTarget target =
                serviceAdminSchoolMapper.selectMutationTarget(univId);
        if (target == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "활성 구독을 찾을 수 없습니다."
            );
        }
        return target;
    }

    private void validateSchedulingTarget(ServiceAdminSchoolDto.MutationTarget target) {
        validateBillingKey(target);
        if (target.getNextBillingAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "다음 결제일이 없어 플랜을 변경할 수 없습니다."
            );
        }
    }

    private void validateBillingKey(ServiceAdminSchoolDto.MutationTarget target) {
        if (target.getBillingKeyId() == null
                || target.getPortoneBillingKey() == null
                || target.getPortoneBillingKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "등록된 정기결제 수단이 없습니다."
            );
        }
    }

    private void restorePreviousSchedule(ServiceAdminSchoolDto.MutationTarget target) {
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
                    serviceAdminSchoolMapper.replaceReadyPaymentSchedule(
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSubscriptionStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!SUBSCRIPTION_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 구독 상태입니다."
            );
        }
        return normalized;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "NAME_ASC";
        }
        String normalized = sort.trim().toUpperCase();
        if (!SORT_OPTIONS.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지원하지 않는 정렬 방식입니다."
            );
        }
        return normalized;
    }

    private String buildOrderName(String planName) {
        return "UnivUs " + planName + " subscription";
    }

    private String createMerchantUid() {
        return "sub_renew_"
                + java.time.LocalDateTime.now(SUBSCRIPTION_ZONE)
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
