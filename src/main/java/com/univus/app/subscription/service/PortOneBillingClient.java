package com.univus.app.subscription.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class PortOneBillingClient {

    private static final String PORTONE_API_BASE_URL = "https://api.portone.io";

    @Value("${portone.subscription.api-secret}")
    private String apiSecret;

    private final RestClient restClient = RestClient.builder()
            .baseUrl(PORTONE_API_BASE_URL)
            .build();

    public PortOneBillingKey getBillingKey(String billingKey) {
        PortOneBillingKey response = restClient.get()
                .uri("/billing-keys/{billingKey}", billingKey)
                .header("Authorization", "PortOne " + apiSecret)
                .retrieve()
                .body(PortOneBillingKey.class);

        if (response == null) {
            throw new IllegalStateException("PortOne billing key lookup failed.");
        }

        return response;
    }

    public void payWithBillingKey(
            String paymentId,
            String billingKey,
            String orderName,
            Long amount,
            Long memberId
    ) {
        restClient.post()
                .uri("/payments/{paymentId}/billing-key", paymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .body(Map.of(
                        "billingKey", billingKey,
                        "orderName", orderName,
                        "customer", Map.of("id", String.valueOf(memberId)),
                        "amount", Map.of("total", amount),
                        "currency", "KRW"
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public String schedulePayment(
            String paymentId,
            String billingKey,
            String orderName,
            Long amount,
            Long memberId,
            OffsetDateTime timeToPay
    ) {
        CreatePaymentScheduleResponse response = restClient.post()
                .uri("/payments/{paymentId}/schedule", paymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .body(Map.of(
                        "payment", Map.of(
                                "billingKey", billingKey,
                                "orderName", orderName,
                                "customer", Map.of("id", String.valueOf(memberId)),
                                "amount", Map.of("total", amount),
                                "currency", "KRW"
                        ),
                        "timeToPay", timeToPay.toString()
                ))
                .retrieve()
                .body(CreatePaymentScheduleResponse.class);

        if (response == null
                || response.getSchedule() == null
                || response.getSchedule().getId() == null
                || response.getSchedule().getId().isBlank()) {
            throw new IllegalStateException("PortOne payment schedule response is invalid.");
        }
        return response.getSchedule().getId();
    }

    public void revokePaymentSchedule(String scheduleId) {
        revokePaymentSchedules(Map.of("scheduleIds", List.of(scheduleId)));
    }

    public void revokePaymentSchedulesByBillingKey(String billingKey) {
        revokePaymentSchedules(Map.of("billingKey", billingKey));
    }

    private void revokePaymentSchedules(Map<String, Object> request) {
        restClient.method(HttpMethod.DELETE)
                .uri("/payment-schedules")
                .header("Authorization", "PortOne " + apiSecret)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Getter
    public static class PortOneBillingKey {

        private String billingKey;
        private String status;
    }

    @Getter
    public static class CreatePaymentScheduleResponse {
        private PaymentSchedule schedule;
    }

    @Getter
    public static class PaymentSchedule {
        private String id;
    }
}
