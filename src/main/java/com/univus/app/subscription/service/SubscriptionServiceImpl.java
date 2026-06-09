package com.univus.app.subscription.service;

import com.univus.app.security.JwtTokenProvider;
import com.univus.app.subscription.dto.*;
import com.univus.app.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    // SUBSCRIPTIONS.STATUS에 저장할 결제 대기 상태입니다.
    // 실제 결제가 검증되기 전까지는 ACTIVE가 아니라 PENDING으로 둡니다.
    private static final String SUBSCRIPTION_STATUS_PENDING = "PENDING";

    // SUBSCRIPTION_PAYMENT_HISTORIES.STATUS에 저장할 결제 준비 상태입니다.
    // PortOne 결제 완료 검증 API에서 이후 PAID 또는 FAILED로 바꿀 예정입니다.
    private static final String PAYMENT_STATUS_READY = "READY";

    // merchantUid에 들어갈 시간 문자열 포맷입니다.
    // 같은 시각 요청이 겹쳐도 UUID suffix를 붙여 중복 가능성을 낮춥니다.
    private static final DateTimeFormatter MERCHANT_UID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SubscriptionMapper subscriptionMapper;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_FAILED = "FAILED";

    // 구독 결제 성공 후 회원에게 부여할 학교 관리자 역할입니다.
    private static final String MEMBER_ROLE_ADMIN = "ADM";

    // 활성 구독 플랜 목록을 조회합니다.
    // DELETED_AT이 없는 플랜만 mapper에서 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponseDto> getActivePlans() {
        return subscriptionMapper.findActivePlans();
    }

    // 결제창을 띄우기 전, 우리 DB에 결제 준비 데이터를 먼저 만듭니다.
    // 생성 순서:
    // 1. 플랜 검증
    // 2. 회원/학교 중복 구독 방지
    // 3. UNIVERSITY 생성
    // 4. SUBSCRIPTIONS PENDING 생성
    // 5. SUBSCRIPTION_PAYMENT_HISTORIES READY 생성
    // 6. 프론트에 merchantUid와 amount 반환
    @Override
    @Transactional
    public SubscriptionPrepareResponseDto prepareSubscription(
            Long memberId,
            SubscriptionPrepareRequestDto request
    ) {
        validatePrepareRequest(memberId, request);

        // 결제 금액은 프론트 요청값을 믿지 않고 DB의 플랜 가격을 기준으로 사용합니다.
        SubscriptionPlanResponseDto plan =
                subscriptionMapper.findActivePlanById(request.getPlanId());

        if (plan == null) {
            throw new IllegalArgumentException("존재하지 않는 구독 플랜입니다.");
        }

        // 같은 회원이 결제 대기 또는 활성 구독을 중복 생성하지 못하게 막습니다.
        if (subscriptionMapper.countPendingOrActiveSubscriptionByMemberId(memberId) > 0) {
            throw new IllegalStateException("이미 진행 중이거나 활성화된 구독이 있습니다.");
        }

        String univName = request.getUnivName().trim();

        // 같은 학교명으로 결제 대기 또는 활성 구독이 이미 있으면 중복 등록을 막습니다.
        if (subscriptionMapper.countPendingOrActiveSubscriptionByUnivName(univName) > 0) {
            throw new IllegalStateException("이미 진행 중이거나 활성화된 학교 구독이 있습니다.");
        }

        // 구독 신청 과정에서 학교 정보를 먼저 생성합니다.
        // insert 후 selectKey로 university.univId가 채워집니다.
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
        // billingKeyId와 nextBillingAt은 결제 검증/빌링키 발급 후 채울 예정입니다.
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
        // 이후 결제 검증 API에서 merchantUid로 이 결제 이력을 다시 찾게 됩니다.
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

        // 프론트는 이 응답의 merchantUid와 amount로 PortOne 결제창을 호출합니다.
        return SubscriptionPrepareResponseDto.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .paymentHistoryId(paymentHistory.getHistoryId())
                .merchantUid(paymentHistory.getMerchantUid())
                .amount(plan.getPrice())
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .build();
    }

    // PortOne 결제 완료 후 호출되는 검증 메서드입니다.
    // 현재 버전은 PortOne 서버 API 실제 조회 전 임시 구현입니다.
    // DB에 저장된 READY 결제 이력과 PENDING 구독을 찾아 PAID/ACTIVE로 변경합니다.
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

        // TODO: 다음 단계에서 PortOne 서버 API로 실제 결제 상태와 금액을 검증해야 합니다.
        // 현재는 portonePaymentId가 존재하면 결제 성공으로 간주하는 임시 검증입니다.
        LocalDateTime paidAt = LocalDateTime.now();
        LocalDateTime nextBillingAt = paidAt.plusMonths(1);

        requireSingleRow(
                subscriptionMapper.markPaymentHistoryPaid(
                        target.getPaymentHistoryId(),
                        request.getPortonePaymentId()
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
        // 프론트는 기존 GUEST accessToken을 이 값으로 교체해야 합니다.
        String accessToken = jwtTokenProvider.createAccessToken(target.getMemberId(), MEMBER_ROLE_ADMIN);

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

    // 결제 검증 요청의 필수값을 확인합니다.
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

    // 결제 준비 요청의 필수값을 검증합니다.
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

    // PortOne 결제 요청에 사용할 주문번호를 만듭니다.
    // 예: sub_20260609143015999_a1b2c3d4
    private String createMerchantUid() {
        String timestamp = LocalDateTime.now().format(MERCHANT_UID_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "sub_" + timestamp + "_" + suffix;
    }

    // 빈 문자열은 DB에 빈 값으로 넣지 않고 null로 정리합니다.
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // insert/update 결과가 정확히 1건인지 확인합니다.
    private void requireSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new IllegalStateException(message);
        }
    }
}