package com.univus.app.subscription.controller;

import com.univus.app.subscription.dto.*;
import com.univus.app.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 구독 플랜 목록 조회 API입니다.
    // 로그인하지 않은 사용자도 결제 전 플랜을 볼 수 있어야 하므로,
    // 이 경로는 나중에 SpringSecurityConfig에서 permitAll로 열어줍니다.
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponseDto>> getActivePlans() {
        return ResponseEntity.ok(subscriptionService.getActivePlans());
    }

    // PortOne 결제창 호출에 필요한 공개 식별값만 반환합니다.
    // API secret은 서버 내부에서만 사용하며 응답에 포함하지 않습니다.
    @GetMapping("/payments/config")
    public ResponseEntity<SubscriptionPaymentConfigResponseDto> getPaymentConfig() {
        return ResponseEntity.ok(subscriptionService.getPaymentConfig());
    }

    // 구독 결제 준비 API입니다.
    // 로그인한 사용자의 memberId를 JWT 인증 결과에서 받아옵니다.
    // 이 API는 실제 결제를 완료하는 API가 아닙니다.
    // PortOne 결제창을 띄우기 전에 DB에 PENDING 신청/구독과 READY 결제 이력을 만들고,
    // 프론트에 merchantUid와 amount를 내려주는 역할만 합니다.
    @PostMapping("/prepare")
    public ResponseEntity<?> prepareSubscription(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SubscriptionPrepareRequestDto request
    ) {
        try {
            SubscriptionPrepareResponseDto response =
                    subscriptionService.prepareSubscription(memberId, request);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            // 요청값 누락, 잘못된 플랜 ID 같은 클라이언트 오류입니다.
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            // 이미 구독이 있거나 중복 학교 구독이 있는 경우입니다.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    // PortOne 결제 완료 후 프론트가 호출하는 결제 검증 API입니다.
    // PortOne 서버에서 결제 상태와 금액을 검증한 뒤에만 학교를 생성합니다.
    // 성공 시 결제 이력은 PAID, 구독은 ACTIVE, 신청은 COMPLETED로 변경합니다.
    // POST /api/subscriptions/payments/verify
    @PostMapping("/payments/verify")
    public ResponseEntity<?> verifySubscriptionPayment(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SubscriptionPaymentVerifyRequestDto request
    ) {
        try {
            SubscriptionPaymentVerifyResponseDto response =
                    subscriptionService.verifySubscriptionPayment(memberId, request);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            // 요청값 누락, 결제 이력 없음, 회원 불일치 같은 클라이언트 오류입니다.
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            // 이미 처리된 결제이거나 상태가 맞지 않는 경우입니다.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    // PortOne 결제창에서 사용자가 결제를 취소하거나 결제 진행에 실패했을 때 호출하는 API입니다.
    // prepare 단계에서 생성된 신청, 결제 이력, 구독을 함께 닫습니다.
    // 이렇게 해야 같은 사용자가 다시 구독 신청을 시도할 수 있습니다.
    // POST /api/subscriptions/payments/cancel
    @PostMapping("/payments/cancel")
    public ResponseEntity<?> cancelSubscriptionPayment(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SubscriptionPaymentCancelRequestDto request
    ) {
        try {
            subscriptionService.cancelPreparedSubscriptionPayment(memberId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "결제 요청이 취소되었습니다."
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
}
