package com.univus.app.member.service;

public interface RecoveryVerificationProvider {

    VerificationInstruction issueChallenge(String phoneNumber, String verificationText);

    boolean verify(String phoneNumber, String verificationText);

    record VerificationInstruction(String recipientNumber, String messageText) {
    }
}
