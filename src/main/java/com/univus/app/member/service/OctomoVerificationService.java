package com.univus.app.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class OctomoVerificationService implements RecoveryVerificationProvider {

    public static final String OCTOMO_RECEIVER_NUMBER = "16663538";
    private static final String OCTOMO_API_BASE_URL = "https://api.octoverse.kr";

    @Value("${octomo.api-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.builder()
            .baseUrl(OCTOMO_API_BASE_URL)
            .build();

    @Override
    public VerificationInstruction issueChallenge(String phoneNumber, String verificationText) {
        return new VerificationInstruction(OCTOMO_RECEIVER_NUMBER, verificationText);
    }

    @Override
    public boolean verify(String phoneNumber, String messageText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "옥토모 인증 설정이 완료되지 않았습니다."
            );
        }

        try {
            ExistsResponse response = restClient.post()
                    .uri("/octomo/v1/public/message/exists")
                    .header("Authorization", "Octomo " + apiKey)
                    .body(Map.of(
                            "mobileNum", phoneNumber,
                            "text", messageText,
                            "withinMinutes", 5
                    ))
                    .retrieve()
                    .body(ExistsResponse.class);

            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "옥토모 인증 확인에 실패했습니다. 잠시 후 다시 시도해주세요."
                );
            }
            return response.exists();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "옥토모 인증 확인에 실패했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    private record ExistsResponse(boolean exists) {
    }
}
