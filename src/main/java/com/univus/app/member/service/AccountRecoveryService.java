package com.univus.app.member.service;

import com.univus.app.member.dto.AccountRecoveryDto;

public interface AccountRecoveryService {

    AccountRecoveryDto.VerificationChallengeResponse sendIdRecoveryCode(
            AccountRecoveryDto.IdentityRequest request
    );

    AccountRecoveryDto.IdVerifyResponse verifyIdRecoveryCode(AccountRecoveryDto.IdVerifyRequest request);

    AccountRecoveryDto.VerificationChallengeResponse sendPasswordRecoveryCode(
            AccountRecoveryDto.PasswordIdentityRequest request
    );

    AccountRecoveryDto.PasswordVerifyResponse verifyPasswordRecoveryCode(
            AccountRecoveryDto.PasswordVerifyRequest request
    );

    void resetPassword(AccountRecoveryDto.ResetPasswordRequest request);
}
