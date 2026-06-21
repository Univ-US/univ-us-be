package com.univus.app.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class AccountRecoveryDto {

    @Getter
    @Setter
    public static class IdentityRequest {
        @NotBlank
        private String memberName;

        @NotBlank
        private String phoneNumber;

        @NotBlank
        private String birth;
    }

    @Getter
    @Setter
    public static class PasswordIdentityRequest extends IdentityRequest {
        @NotBlank
        private String loginId;
    }

    @Getter
    @Setter
    public static class IdVerifyRequest extends IdentityRequest {
    }

    @Getter
    @Setter
    public static class PasswordVerifyRequest extends PasswordIdentityRequest {
    }

    @Getter
    @Setter
    public static class VerificationChallengeResponse {
        private String recipientNumber;
        private String messageText;
        private long expiresInSeconds;
    }

    @Getter
    @Setter
    public static class ResetPasswordRequest {
        @NotBlank
        private String resetToken;

        @NotBlank
        private String password;
    }

    @Getter
    @Setter
    public static class LoginIdItem {
        private String loginId;
        private String univName;
    }

    @Getter
    @Setter
    public static class IdVerifyResponse {
        private List<LoginIdItem> accounts;
    }

    @Getter
    @Setter
    public static class PasswordVerifyResponse {
        private String resetToken;
    }
}
