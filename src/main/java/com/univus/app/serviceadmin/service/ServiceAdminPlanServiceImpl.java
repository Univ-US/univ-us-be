package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminPlanDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminPlanMapper;
import com.univus.app.subscription.service.PortOneBillingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAdminPlanServiceImpl implements ServiceAdminPlanService {

    private static final ZoneId SUBSCRIPTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final ServiceAdminPlanMapper serviceAdminPlanMapper;
    private final PortOneBillingClient portOneBillingClient;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminPlanDto.Response getPlans() {
        return new ServiceAdminPlanDto.Response(
                serviceAdminPlanMapper.selectPlans(),
                serviceAdminPlanMapper.selectCurrentMonthRevenue()
        );
    }

    @Transactional
    @Override
    public ServiceAdminPlanDto.Plan createPlan(
            ServiceAdminPlanDto.SaveRequest request
    ) {
        normalize(request);
        validateDuplicateName(request.getPlanName(), null);
        requireSingleRow(
                serviceAdminPlanMapper.insertPlan(request),
                "구독 플랜을 생성하지 못했습니다."
        );
        return getPlan(request.getPlanId());
    }

    @Override
    public ServiceAdminPlanDto.Plan updatePlan(
            Long planId,
            ServiceAdminPlanDto.SaveRequest request
    ) {
        ServiceAdminPlanDto.Plan current = getPlan(planId);
        if (!"ACTIVE".equals(current.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "비활성 플랜은 수정할 수 없습니다. 먼저 다시 활성화해 주세요."
            );
        }

        normalize(request);
        validateDuplicateName(request.getPlanName(), planId);

        boolean scheduleChanged =
                !current.getPrice().equals(request.getPrice())
                        || !current.getPlanName().equals(request.getPlanName());
        List<ScheduleReplacement> replacements = new ArrayList<>();

        try {
            if (scheduleChanged) {
                for (ServiceAdminPlanDto.ReadyPaymentTarget target
                        : serviceAdminPlanMapper.selectReadyPayments(current.getPlanName())) {
                    replacements.add(replaceSchedule(
                            target,
                            request.getPlanName(),
                            request.getPrice()
                    ));
                }
            }

            transactionTemplate.executeWithoutResult(status -> requireSingleRow(
                    serviceAdminPlanMapper.updatePlan(planId, request),
                    "구독 플랜을 수정하지 못했습니다."
            ));
        } catch (RuntimeException exception) {
            compensate(replacements);
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "예약 결제와 플랜 정보를 동기화하지 못했습니다.",
                    exception
            );
        }

        return getPlan(planId);
    }

    @Transactional
    @Override
    public ServiceAdminPlanDto.Plan changeStatus(Long planId, String requestedStatus) {
        ServiceAdminPlanDto.Plan plan = getPlan(planId);
        String status = normalizeStatus(requestedStatus);
        if (status.equals(plan.getStatus())) {
            return plan;
        }

        int rows = "ACTIVE".equals(status)
                ? serviceAdminPlanMapper.activatePlan(planId)
                : serviceAdminPlanMapper.deactivatePlan(planId);
        requireSingleRow(rows, "구독 플랜 상태를 변경하지 못했습니다.");
        return getPlan(planId);
    }

    private ScheduleReplacement replaceSchedule(
            ServiceAdminPlanDto.ReadyPaymentTarget target,
            String newPlanName,
            Long newAmount
    ) {
        validateReadyPayment(target);
        String newMerchantUid = createMerchantUid();
        String newScheduleId = null;
        boolean oldScheduleRevoked = false;

        try {
            newScheduleId = portOneBillingClient.schedulePayment(
                    newMerchantUid,
                    target.getPortoneBillingKey(),
                    buildOrderName(newPlanName),
                    newAmount,
                    target.getMemberId(),
                    target.getNextBillingAt()
                            .atZone(SUBSCRIPTION_ZONE)
                            .toOffsetDateTime()
            );
            portOneBillingClient.revokePaymentSchedule(
                    target.getPortoneScheduleId()
            );
            oldScheduleRevoked = true;

            String savedScheduleId = newScheduleId;
            transactionTemplate.executeWithoutResult(status -> requireSingleRow(
                    serviceAdminPlanMapper.replaceReadyPaymentSchedule(
                            target.getHistoryId(),
                            newMerchantUid,
                            savedScheduleId,
                            newAmount,
                            newPlanName
                    ),
                    "예약 결제 정보를 갱신하지 못했습니다."
            ));
            return new ScheduleReplacement(target, newScheduleId);
        } catch (RuntimeException exception) {
            revokeQuietly(newScheduleId);
            if (oldScheduleRevoked) {
                restore(target);
            }
            throw exception;
        }
    }

    private void compensate(List<ScheduleReplacement> replacements) {
        RuntimeException compensationFailure = null;
        for (int index = replacements.size() - 1; index >= 0; index--) {
            ScheduleReplacement replacement = replacements.get(index);
            revokeQuietly(replacement.newScheduleId());
            try {
                restore(replacement.original());
            } catch (RuntimeException exception) {
                compensationFailure = exception;
            }
        }
        if (compensationFailure != null) {
            throw compensationFailure;
        }
    }

    private void restore(ServiceAdminPlanDto.ReadyPaymentTarget target) {
        try {
            String merchantUid = createMerchantUid();
            String scheduleId = portOneBillingClient.schedulePayment(
                    merchantUid,
                    target.getPortoneBillingKey(),
                    buildOrderName(target.getPlanName()),
                    target.getAmount(),
                    target.getMemberId(),
                    target.getNextBillingAt()
                            .atZone(SUBSCRIPTION_ZONE)
                            .toOffsetDateTime()
            );
            transactionTemplate.executeWithoutResult(status -> requireSingleRow(
                    serviceAdminPlanMapper.replaceReadyPaymentSchedule(
                            target.getHistoryId(),
                            merchantUid,
                            scheduleId,
                            target.getAmount(),
                            target.getPlanName()
                    ),
                    "기존 예약 결제 복구 정보를 저장하지 못했습니다."
            ));
        } catch (RuntimeException restoreException) {
            log.error(
                    "Failed to restore plan payment schedule. historyId={}",
                    target.getHistoryId(),
                    restoreException
            );
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "예약 결제 복구에 실패했습니다. 결제 이력을 확인해 주세요.",
                    restoreException
            );
        }
    }

    private void validateReadyPayment(
            ServiceAdminPlanDto.ReadyPaymentTarget target
    ) {
        if (target.getBillingKeyId() == null
                || isBlank(target.getPortoneBillingKey())
                || !"ACTIVE".equals(target.getBillingKeyStatus())
                || isBlank(target.getPortoneScheduleId())
                || target.getNextBillingAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "변경할 수 없는 예약 결제가 있습니다. 결제 수단과 예약 ID를 확인해 주세요."
            );
        }
    }

    private ServiceAdminPlanDto.Plan getPlan(Long planId) {
        ServiceAdminPlanDto.Plan plan = serviceAdminPlanMapper.selectPlan(planId);
        if (plan == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "구독 플랜을 찾을 수 없습니다."
            );
        }
        return plan;
    }

    private void normalize(ServiceAdminPlanDto.SaveRequest request) {
        request.setPlanName(request.getPlanName().trim());
        request.setDescription(request.getDescription().trim());
    }

    private void validateDuplicateName(String planName, Long excludePlanId) {
        if (serviceAdminPlanMapper.countDuplicateName(planName, excludePlanId) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 플랜명입니다."
            );
        }
    }

    private String normalizeStatus(String status) {
        String normalized =
                status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "플랜 상태는 ACTIVE 또는 INACTIVE만 사용할 수 있습니다."
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
        return "UnivUs " + planName + " subscription";
    }

    private void revokeQuietly(String scheduleId) {
        if (isBlank(scheduleId)) {
            return;
        }
        try {
            portOneBillingClient.revokePaymentSchedule(scheduleId);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to revoke replacement schedule. scheduleId={}",
                    scheduleId,
                    exception
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

    private record ScheduleReplacement(
            ServiceAdminPlanDto.ReadyPaymentTarget original,
            String newScheduleId
    ) {
    }
}
