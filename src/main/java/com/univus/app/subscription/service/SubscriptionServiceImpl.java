package com.univus.app.subscription.service;

import com.univus.app.security.JwtTokenProvider;
import com.univus.app.subscription.dto.*;
import com.univus.app.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String SUBSCRIPTION_STATUS_PENDING = "PENDING";
    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String PAYMENT_STATUS_READY = "READY";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_FAILED = "FAILED";
    private static final String APPLICATION_STATUS_PENDING = "PENDING";
    private static final String APPLICATION_STATUS_FAILED = "FAILED";

    private static final String MEMBER_ROLE_ADMIN = "ADM";

    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SubscriptionMapper subscriptionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PortOnePaymentClient portOnePaymentClient;
    private final SubscriptionPaymentFailureRecorder failureRecorder;

    @Value("${portone.subscription.store-id:}")
    private String portOneStoreId;

    @Value("${portone.subscription.channel-key:}")
    private String portOneChannelKey;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponseDto> getActivePlans() {
        return subscriptionMapper.findActivePlans();
    }

    @Override
    public SubscriptionPaymentConfigResponseDto getPaymentConfig() {
        return SubscriptionPaymentConfigResponseDto.builder()
                .storeId(portOneStoreId)
                .channelKey(portOneChannelKey)
                .build();
    }

    @Override
    @Transactional
    public SubscriptionPrepareResponseDto prepareSubscription(
            Long memberId,
            SubscriptionPrepareRequestDto request
    ) {
        validatePrepareRequest(memberId, request);

        // 결제 금액은 프론트 요청값이 아니라 DB에 저장된 플랜 가격을 기준으로 사용합니다.
        SubscriptionPlanResponseDto plan =
                subscriptionMapper.findActivePlanById(request.getPlanId());

        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 구독 플랜입니다.");
        }

        // 같은 회원이 이미 결제 대기 또는 구독 중이면 중복 구독 생성을 막습니다.
        if (subscriptionMapper.countPendingOrActiveSubscriptionByMemberId(memberId) > 0) {
            throw new IllegalStateException("이미 진행 중이거나 활성화된 구독이 있습니다.");
        }

        String univName = request.getUnivName().trim();

        // 기존 방식으로 생성된 학교에 PENDING/ACTIVE 구독이 있으면 중복 등록을 막습니다.
        if (subscriptionMapper.countPendingOrActiveSubscriptionByUnivName(univName) > 0) {
            throw new IllegalStateException("이미 진행 중이거나 활성화된 학교 구독이 있습니다.");
        }

        // 아직 결제가 끝나지 않은 같은 학교 신청이 있으면 중복 신청을 막습니다.
        if (subscriptionMapper.countPendingSubscriptionApplicationByUnivName(univName) > 0) {
            throw new IllegalStateException("이미 진행 중인 학교 구독 신청이 있습니다.");
        }

        // 실제 결제 성공 전이므로 구독은 PENDING 상태로 생성합니다.
        SubscriptionInsertDto subscription = SubscriptionInsertDto.builder()
                .planId(plan.getPlanId())
                .billingKeyId(null)
                .memberId(memberId)
                .univId(null)
                .status(SUBSCRIPTION_STATUS_PENDING)
                .startedAt(LocalDateTime.now())
                .nextBillingAt(null)
                .endedAt(null)
                .build();

        requireSingleRow(
                subscriptionMapper.insertSubscription(subscription),
                "구독 정보 생성에 실패했습니다."
        );

        // 결제 성공 전 학교 정보는 UNIVERSITY가 아닌 신청 테이블에 보관합니다.
        SubscriptionApplicationDto application = SubscriptionApplicationDto.builder()
                .memberId(memberId)
                .subscriptionId(subscription.getSubscriptionId())
                .planId(plan.getPlanId())
                .univName(univName)
                .sido(request.getSido().trim())
                .address(request.getAddress().trim())
                .schoolPhone(request.getSchoolPhone().trim())
                .homepage(request.getHomepage().trim())
                .status(APPLICATION_STATUS_PENDING)
                .build();

        requireSingleRow(
                subscriptionMapper.insertSubscriptionApplication(application),
                "구독 신청 정보 생성에 실패했습니다."
        );

        // 프론트가 PortOne 결제를 시작할 때 사용할 merchantUid를 먼저 저장합니다.
        SubscriptionPaymentHistoryInsertDto paymentHistory =
                SubscriptionPaymentHistoryInsertDto.builder()
                        .subscriptionId(subscription.getSubscriptionId())
                        .univId(null)
                        .memberId(memberId)
                        .billingKeyId(null)
                        .status(PAYMENT_STATUS_READY)
                        .merchantUid(createMerchantUid())
                        .portonePaymentId(null)
                        .paidAt(null)
                        .failReason(null)
                        .amount(plan.getPrice())
                        .build();

        requireSingleRow(
                subscriptionMapper.insertPaymentHistory(paymentHistory),
                "결제 이력 생성에 실패했습니다."
        );

        return SubscriptionPrepareResponseDto.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .paymentHistoryId(paymentHistory.getHistoryId())
                .merchantUid(paymentHistory.getMerchantUid())
                .amount(plan.getPrice())
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .build();
    }

    @Override
    @Transactional
    public SubscriptionPaymentVerifyResponseDto verifySubscriptionPayment(
            Long memberId,
            SubscriptionPaymentVerifyRequestDto request
    ) {
        validateVerifyRequest(memberId, request);

        SubscriptionPaymentVerifyTargetDto target =
                subscriptionMapper.findPaymentVerifyTargetByMerchantUid(request.getMerchantUid());

        if (target == null) {
            throw new IllegalArgumentException("결제 준비 이력을 찾을 수 없습니다.");
        }

        if (!memberId.equals(target.getMemberId())) {
            throw new IllegalArgumentException("결제 요청 회원이 일치하지 않습니다.");
        }

        if (!PAYMENT_STATUS_READY.equals(target.getPaymentStatus())) {
            throw new IllegalStateException("이미 처리된 결제 이력입니다.");
        }

        if (!SUBSCRIPTION_STATUS_PENDING.equals(target.getSubscriptionStatus())) {
            throw new IllegalStateException("결제 대기 상태의 구독이 아닙니다.");
        }

        boolean hasApplication = target.getApplicationId() != null;
        boolean isLegacyPreparedPayment = !hasApplication && target.getUnivId() != null;

        if (!hasApplication && !isLegacyPreparedPayment) {
            throw new IllegalStateException("구독 신청 정보를 찾을 수 없습니다.");
        }

        validateApplicationLink(target);

        if (hasApplication && !APPLICATION_STATUS_PENDING.equals(target.getApplicationStatus())) {
            throw new IllegalStateException("결제 대기 상태의 구독 신청이 아닙니다.");
        }

        PortOnePaymentClient.PortOnePayment portOnePayment;

        try {
            // PortOne V2 서버 API로 실제 결제 내역을 조회합니다.
            // portonePaymentId는 프론트가 PortOne.requestPayment에 넘긴 paymentId입니다.
            portOnePayment = portOnePaymentClient.getPayment(request.getPortonePaymentId());
        } catch (RestClientResponseException e) {
            // PortOne이 404, 401 같은 HTTP 오류를 내려준 경우에도 실패 이력을 DB에 남깁니다.
            markVerificationFailed(
                    target,
                    "PortOne payment lookup failed: " + e.getStatusCode().value()
            );

            throw new IllegalStateException("PortOne 결제 조회에 실패했습니다.");
        } catch (RestClientException e) {
            // 네트워크 오류 등 HTTP 응답 자체를 정상적으로 받지 못한 경우입니다.
            markVerificationFailed(target, "PortOne payment lookup failed.");

            throw new IllegalStateException("PortOne 결제 조회에 실패했습니다.");
        }

        // PortOne의 결제 ID와 프론트가 보낸 paymentId가 같은지 확인합니다.
        if (!request.getPortonePaymentId().equals(portOnePayment.getId())) {
            markVerificationFailed(target, "PortOne paymentId mismatch.");
            throw new IllegalStateException("결제 ID가 일치하지 않습니다.");
        }

        // PortOne 결제 상태가 PAID인지 확인합니다.
        if (!PAYMENT_STATUS_PAID.equals(portOnePayment.getStatus())) {
            markVerificationFailed(
                    target,
                    "PortOne payment status is not PAID: " + portOnePayment.getStatus()
            );
            throw new IllegalStateException("결제가 완료되지 않았습니다.");
        }

        // PortOne 결제 금액과 DB에 저장된 금액을 비교합니다.
        Long portOneAmount = portOnePayment.getAmount() == null
                ? null
                : portOnePayment.getAmount().getTotal();

        if (!target.getAmount().equals(portOneAmount)) {
            markVerificationFailed(target, "PortOne amount mismatch.");
            throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
        }

        // 우리 DB의 merchantUid와 PortOne V2 paymentId가 같은지 확인합니다.
        // 프론트는 PortOne.requestPayment({ paymentId: prepareResponse.merchantUid })로 호출해야 합니다.
        if (!target.getMerchantUid().equals(portOnePayment.getId())) {
            markVerificationFailed(target, "PortOne paymentId does not match merchantUid.");
            throw new IllegalStateException("결제 주문번호가 일치하지 않습니다.");
        }

        // PortOne paidAt이 있으면 그 시각을 사용하고, 없으면 현재 시각을 사용합니다.
        LocalDateTime paidAt = toLocalDateTime(portOnePayment.getPaidAt());
        LocalDateTime nextBillingAt = paidAt.plusMonths(1);

        Long verifiedUnivId = target.getUnivId();

        if (hasApplication) {
            validateApplicationUniversityInfo(target);

            // 신규 흐름에서는 결제 검증이 모두 끝난 뒤에만 실제 학교 데이터를 생성합니다.
            SubscriptionUniversityDto university = SubscriptionUniversityDto.builder()
                    .univName(target.getUnivName())
                    .sido(target.getSido())
                    .address(target.getAddress())
                    .schoolPhone(target.getSchoolPhone())
                    .homepage(target.getHomepage())
                    .build();

            requireSingleRow(
                    subscriptionMapper.insertUniversity(university),
                    "학교 정보 생성에 실패했습니다."
            );

            verifiedUnivId = university.getUnivId();
        }

        requireSingleRow(
                subscriptionMapper.markPaymentHistoryPaid(
                        target.getPaymentHistoryId(),
                        request.getPortonePaymentId(),
                        paidAt,
                        verifiedUnivId
                ),
                "결제 이력 상태 변경에 실패했습니다."
        );

        requireSingleRow(
                subscriptionMapper.activateSubscription(
                        target.getSubscriptionId(),
                        verifiedUnivId,
                        nextBillingAt
                ),
                "구독 활성화에 실패했습니다."
        );

        requireSingleRow(
                subscriptionMapper.updateMemberAsUniversityAdmin(
                        target.getMemberId(),
                        verifiedUnivId
                ),
                "회원 관리자 권한 변경에 실패했습니다."
        );

        if (hasApplication) {
            requireSingleRow(
                    subscriptionMapper.completeSubscriptionApplication(target.getApplicationId()),
                    "구독 신청 완료 처리에 실패했습니다."
            );
        }

        // MEMBER.ROLE이 ADM으로 변경되었으므로 ADM 권한이 들어간 새 accessToken을 발급합니다.
        String accessToken =
                jwtTokenProvider.createAccessToken(target.getMemberId(), MEMBER_ROLE_ADMIN);

        return SubscriptionPaymentVerifyResponseDto.builder()
                .subscriptionId(target.getSubscriptionId())
                .paymentHistoryId(target.getPaymentHistoryId())
                .portonePaymentId(request.getPortonePaymentId())
                .merchantUid(target.getMerchantUid())
                .amount(target.getAmount())
                .paymentStatus(PAYMENT_STATUS_PAID)
                .subscriptionStatus(SUBSCRIPTION_STATUS_ACTIVE)
                .paidAt(paidAt)
                .nextBillingAt(nextBillingAt)
                .memberId(target.getMemberId())
                .univId(verifiedUnivId)
                .role(MEMBER_ROLE_ADMIN)
                .accessToken(accessToken)
                .tokenType(jwtTokenProvider.getTokenType())
                .build();
    }

    @Override
    @Transactional
    public void cancelPreparedSubscriptionPayment(
            Long memberId,
            SubscriptionPaymentCancelRequestDto request
    ) {
        validateCancelRequest(memberId, request);

        SubscriptionPaymentVerifyTargetDto target =
                subscriptionMapper.findPaymentVerifyTargetByMerchantUid(request.getMerchantUid());

        if (target == null) {
            throw new IllegalArgumentException("결제 준비 이력을 찾을 수 없습니다.");
        }

        if (!memberId.equals(target.getMemberId())) {
            throw new IllegalArgumentException("결제 요청 회원이 일치하지 않습니다.");
        }


        if (!SUBSCRIPTION_STATUS_PENDING.equals(target.getSubscriptionStatus())) {
            throw new IllegalStateException("취소할 수 없는 구독 상태입니다.");
        }

        validateApplicationLink(target);

        String reason = request.getReason() == null || request.getReason().trim().isEmpty()
                ? "USER_CANCELLED"
                : request.getReason().trim();

        if (PAYMENT_STATUS_READY.equals(target.getPaymentStatus())) {
            validatePendingApplicationForCancel(target);

            requireSingleRow(
                    subscriptionMapper.markPaymentHistoryCanceled(
                            target.getPaymentHistoryId(),
                            reason
                    ),
                    "결제 이력 취소 처리에 실패했습니다."
            );

            requireSingleRow(
                    subscriptionMapper.cancelSubscription(target.getSubscriptionId()),
                    "구독 취소 처리에 실패했습니다."
            );

            if (target.getApplicationId() != null) {
                requireSingleRow(
                        subscriptionMapper.cancelSubscriptionApplication(
                                target.getApplicationId(),
                                reason
                        ),
                        "구독 신청 취소 처리에 실패했습니다."
                );
            }

            return;
        }

        if (PAYMENT_STATUS_FAILED.equals(target.getPaymentStatus())) {
            validateFailedApplicationForCancel(target);

            requireSingleRow(
                    subscriptionMapper.cancelSubscription(target.getSubscriptionId()),
                    "구독 취소 처리에 실패했습니다."
            );

            if (target.getApplicationId() != null
                    && APPLICATION_STATUS_PENDING.equals(target.getApplicationStatus())) {
                requireSingleRow(
                        subscriptionMapper.failSubscriptionApplication(
                                target.getApplicationId(),
                                reason
                        ),
                        "구독 신청 실패 처리에 실패했습니다."
                );
            }

            return;
        }

        throw new IllegalStateException("취소할 수 없는 결제 상태입니다.");
    }

    private void validateCancelRequest(
            Long memberId,
            SubscriptionPaymentCancelRequestDto request
    ) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 필요합니다.");
        }

        if (request.getMerchantUid() == null || request.getMerchantUid().trim().isEmpty()) {
            throw new IllegalArgumentException("merchantUid가 필요합니다.");
        }
    }

    private void validateVerifyRequest(
            Long memberId,
            SubscriptionPaymentVerifyRequestDto request
    ) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 필요합니다.");
        }

        if (request.getMerchantUid() == null || request.getMerchantUid().trim().isEmpty()) {
            throw new IllegalArgumentException("merchantUid가 필요합니다.");
        }

        if (request.getPortonePaymentId() == null || request.getPortonePaymentId().trim().isEmpty()) {
            throw new IllegalArgumentException("portonePaymentId가 필요합니다.");
        }
    }

    private void validatePrepareRequest(Long memberId, SubscriptionPrepareRequestDto request) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (request == null) {
            throw new IllegalArgumentException("요청 본문이 필요합니다.");
        }

        if (request.getPlanId() == null) {
            throw new IllegalArgumentException("구독 플랜 ID가 필요합니다.");
        }

        if (request.getUnivName() == null || request.getUnivName().trim().isEmpty()) {
            throw new IllegalArgumentException("학교명이 필요합니다.");
        }

        if (request.getSido() == null || request.getSido().trim().isEmpty()) {
            throw new IllegalArgumentException("시도 정보가 필요합니다.");
        }

        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("학교 주소가 필요합니다.");
        }

        if (request.getSchoolPhone() == null || request.getSchoolPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("학교 대표번호가 필요합니다.");
        }

        if (request.getHomepage() == null || request.getHomepage().trim().isEmpty()) {
            throw new IllegalArgumentException("학교 홈페이지가 필요합니다.");
        }
    }

    private void markVerificationFailed(
            SubscriptionPaymentVerifyTargetDto target,
            String failReason
    ) {
        failureRecorder.markFailed(
                target.getPaymentHistoryId(),
                target.getSubscriptionId(),
                target.getApplicationId(),
                failReason
        );
    }

    private void validatePendingApplicationForCancel(
            SubscriptionPaymentVerifyTargetDto target
    ) {
        if (target.getApplicationId() != null
                && !APPLICATION_STATUS_PENDING.equals(target.getApplicationStatus())) {
            throw new IllegalStateException("취소할 수 없는 구독 신청 상태입니다.");
        }
    }

    private void validateFailedApplicationForCancel(
            SubscriptionPaymentVerifyTargetDto target
    ) {
        if (target.getApplicationId() == null) {
            return;
        }

        if (!APPLICATION_STATUS_PENDING.equals(target.getApplicationStatus())
                && !APPLICATION_STATUS_FAILED.equals(target.getApplicationStatus())) {
            throw new IllegalStateException("정리할 수 없는 구독 신청 상태입니다.");
        }
    }

    private void validateApplicationUniversityInfo(
            SubscriptionPaymentVerifyTargetDto target
    ) {
        if (isBlank(target.getUnivName())
                || isBlank(target.getSido())
                || isBlank(target.getAddress())
                || isBlank(target.getSchoolPhone())
                || isBlank(target.getHomepage())) {
            throw new IllegalStateException("학교 신청 정보가 완전하지 않습니다.");
        }
    }

    private void validateApplicationLink(
            SubscriptionPaymentVerifyTargetDto target
    ) {
        if (target.getApplicationId() == null) {
            return;
        }

        if (!target.getMemberId().equals(target.getApplicationMemberId())
                || !target.getPlanId().equals(target.getApplicationPlanId())) {
            throw new IllegalStateException("구독 신청 연결 정보가 일치하지 않습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String createMerchantUid() {
        String timestamp = LocalDateTime.now().format(MERCHANT_UID_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "sub_" + timestamp + "_" + suffix;
    }

    private LocalDateTime toLocalDateTime(String paidAt) {
        if (paidAt == null || paidAt.isBlank()) {
            return LocalDateTime.now();
        }

        return java.time.OffsetDateTime.parse(paidAt)
                .toLocalDateTime();
    }

    private void requireSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
