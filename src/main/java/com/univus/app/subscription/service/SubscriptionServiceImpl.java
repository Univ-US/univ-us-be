package com.univus.app.subscription.service;

import com.univus.app.security.JwtTokenProvider;
import com.univus.app.subscription.dto.*;
import com.univus.app.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
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
    private static final String SUBSCRIPTION_STATUS_CANCELED = "CANCELED";
    private static final String PAYMENT_STATUS_READY = "READY";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_CANCELED = "CANCELED";
    private static final String PAYMENT_STATUS_FAILED = "FAILED";


    private static final String MEMBER_ROLE_ADMIN = "ADM";

    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SubscriptionMapper subscriptionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PortOnePaymentClient portOnePaymentClient;
    private final SubscriptionPaymentFailureRecorder failureRecorder;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponseDto> getActivePlans() {
        return subscriptionMapper.findActivePlans();
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

        // 같은 학교명으로 결제 대기 또는 구독 중인 데이터가 있으면 중복 등록을 막습니다.
        if (subscriptionMapper.countPendingOrActiveSubscriptionByUnivName(univName) > 0) {
            throw new IllegalStateException("이미 진행 중이거나 활성화된 학교 구독이 있습니다.");
        }

        // 구독 신청 과정에서 학교 정보를 먼저 생성합니다.
        SubscriptionUniversityDto university = SubscriptionUniversityDto.builder()
                .univName(univName)
                .sido(trimToNull(request.getSido()))
                .address(trimToNull(request.getAddress()))
                .schoolPhone(trimToNull(request.getSchoolPhone()))
                .homepage(trimToNull(request.getHomepage()))
                .build();

        requireSingleRow(
                subscriptionMapper.insertUniversity(university),
                "학교 정보 생성에 실패했습니다."
        );

        // 실제 결제 성공 전이므로 구독은 PENDING 상태로 생성합니다.
        SubscriptionInsertDto subscription = SubscriptionInsertDto.builder()
                .planId(plan.getPlanId())
                .billingKeyId(null)
                .memberId(memberId)
                .univId(university.getUnivId())
                .status(SUBSCRIPTION_STATUS_PENDING)
                .startedAt(LocalDateTime.now())
                .nextBillingAt(null)
                .endedAt(null)
                .build();

        requireSingleRow(
                subscriptionMapper.insertSubscription(subscription),
                "구독 정보 생성에 실패했습니다."
        );

        // 프론트가 PortOne 결제를 시작할 때 사용할 merchantUid를 먼저 저장합니다.
        SubscriptionPaymentHistoryInsertDto paymentHistory =
                SubscriptionPaymentHistoryInsertDto.builder()
                        .subscriptionId(subscription.getSubscriptionId())
                        .univId(university.getUnivId())
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

        PortOnePaymentClient.PortOnePayment portOnePayment;

        try {
            // PortOne V2 서버 API로 실제 결제 내역을 조회합니다.
            // portonePaymentId는 프론트가 PortOne.requestPayment에 넘긴 paymentId입니다.
            portOnePayment = portOnePaymentClient.getPayment(request.getPortonePaymentId());
        } catch (RestClientResponseException e) {
            // PortOne이 404, 401 같은 HTTP 오류를 내려준 경우에도 실패 이력을 DB에 남깁니다.
            failureRecorder.markFailed(
                    target.getPaymentHistoryId(),
                    "PortOne payment lookup failed: " + e.getStatusCode().value()
            );

            throw new IllegalStateException("PortOne 결제 조회에 실패했습니다.");
        } catch (RestClientException e) {
            // 네트워크 오류 등 HTTP 응답 자체를 정상적으로 받지 못한 경우입니다.
            failureRecorder.markFailed(
                    target.getPaymentHistoryId(),
                    "PortOne payment lookup failed."
            );

            throw new IllegalStateException("PortOne 결제 조회에 실패했습니다.");
        }

        // PortOne의 결제 ID와 프론트가 보낸 paymentId가 같은지 확인합니다.
        if (!request.getPortonePaymentId().equals(portOnePayment.getId())) {
            failureRecorder.markFailed(
                    target.getPaymentHistoryId(),
                    "PortOne paymentId mismatch."
            );
            throw new IllegalStateException("결제 ID가 일치하지 않습니다.");
        }

        // PortOne 결제 상태가 PAID인지 확인합니다.
        if (!PAYMENT_STATUS_PAID.equals(portOnePayment.getStatus())) {
            failureRecorder.markFailed(
                    target.getPaymentHistoryId(),
                    "PortOne payment status is not PAID: " + portOnePayment.getStatus()
            );
            throw new IllegalStateException("결제가 완료되지 않았습니다.");
        }

        // PortOne 결제 금액과 DB에 저장된 금액을 비교합니다.
        Long portOneAmount = portOnePayment.getAmount() == null
                ? null
                : portOnePayment.getAmount().getTotal();

        if (!target.getAmount().equals(portOneAmount)) {
            failureRecorder.markFailed(
                    target.getPaymentHistoryId(),
                    "PortOne amount mismatch."
            );
            throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
        }

        // 우리 DB의 merchantUid와 PortOne V2 paymentId가 같은지 확인합니다.
        // 프론트는 PortOne.requestPayment({ paymentId: prepareResponse.merchantUid })로 호출해야 합니다.
        if (!target.getMerchantUid().equals(portOnePayment.getId())) {
            failureRecorder.markFailed(
                    target.getPaymentHistoryId(),
                    "PortOne paymentId does not match merchantUid."
            );
            throw new IllegalStateException("결제 주문번호가 일치하지 않습니다.");
        }

        // PortOne paidAt이 있으면 그 시각을 사용하고, 없으면 현재 시각을 사용합니다.
        LocalDateTime paidAt = toLocalDateTime(portOnePayment.getPaidAt());
        LocalDateTime nextBillingAt = paidAt.plusMonths(1);

        requireSingleRow(
                subscriptionMapper.markPaymentHistoryPaid(
                        target.getPaymentHistoryId(),
                        request.getPortonePaymentId(),
                        paidAt
                ),
                "결제 이력 상태 변경에 실패했습니다."
        );

        requireSingleRow(
                subscriptionMapper.activateSubscription(
                        target.getSubscriptionId(),
                        nextBillingAt
                ),
                "구독 활성화에 실패했습니다."
        );

        requireSingleRow(
                subscriptionMapper.updateMemberAsUniversityAdmin(
                        target.getMemberId(),
                        target.getUnivId()
                ),
                "회원 관리자 권한 변경에 실패했습니다."
        );

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
                .univId(target.getUnivId())
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

        String reason = request.getReason() == null || request.getReason().trim().isEmpty()
                ? "USER_CANCELLED"
                : request.getReason().trim();

        if (PAYMENT_STATUS_READY.equals(target.getPaymentStatus())) {
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

            return;
        }

        if (PAYMENT_STATUS_FAILED.equals(target.getPaymentStatus())) {
            requireSingleRow(
                    subscriptionMapper.cancelSubscription(target.getSubscriptionId()),
                    "구독 취소 처리에 실패했습니다."
            );

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
    }

    private String createMerchantUid() {
        String timestamp = LocalDateTime.now().format(MERCHANT_UID_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "sub_" + timestamp + "_" + suffix;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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