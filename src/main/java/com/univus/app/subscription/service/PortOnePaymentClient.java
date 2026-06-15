package com.univus.app.subscription.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PortOnePaymentClient {

    private static final String PORTONE_API_BASE_URL = "https://api.portone.io";

    @Value("${portone.subscription.api-secret}")
    private String apiSecret;

    private final RestClient restClient = RestClient.builder()
            .baseUrl(PORTONE_API_BASE_URL)
            .build();

    // PortOne V2 결제 단건을 조회합니다.
    // 결제 목록 조회용이 아니라, verify API에서 특정 paymentId 하나를 검증하기 위한 용도입니다.
    public PortOnePayment getPayment(String paymentId) {
        PortOnePayment response = restClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .retrieve()
                .body(PortOnePayment.class);

        if (response == null) {
            throw new IllegalStateException("PortOne 결제 조회에 실패했습니다.");
        }

        return response;
    }

    public PortOneCancellation cancelPayment(
            String paymentId,
            Long amount,
            String reason
    ) {
        CancelPaymentResponse response = restClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .body(Map.of(
                        "amount", amount,
                        "reason", reason
                ))
                .retrieve()
                .body(CancelPaymentResponse.class);

        if (response == null
                || response.getCancellation() == null
                || response.getCancellation().getId() == null) {
            throw new IllegalStateException("PortOne cancellation response is invalid.");
        }
        return response.getCancellation();
    }

    @Getter
    public static class CancelPaymentResponse {
        private PortOneCancellation cancellation;
    }

    @Getter
    public static class PortOneCancellation {
        private String id;
        private String status;
        private String cancelledAt;
    }

    @Getter
    public static class PortOnePayment {

        // PortOne V2 결제 ID입니다.
        // 프론트가 PortOne.requestPayment에 넘긴 paymentId와 같습니다.
        private String id;

        // 결제 상태입니다.
        // 성공 결제는 PAID여야 합니다.
        private String status;

        // 결제 금액 정보입니다.
        private Amount amount;

        // 결제 완료 시각입니다.
        // ISO-8601 문자열로 내려옵니다.
        private String paidAt;

        // 결제 채널 정보입니다.
        private Channel channel;

        @Getter
        public static class Amount {

            // 총 결제 금액입니다.
            // 우리 DB의 amount와 비교합니다.
            private Long total;
        }

        @Getter
        public static class Channel {

            // 결제 채널 키입니다.
            // 필요하면 우리가 설정한 channelKey와 비교할 수 있습니다.
            @JsonProperty("key")
            private String key;
        }
    }
}
