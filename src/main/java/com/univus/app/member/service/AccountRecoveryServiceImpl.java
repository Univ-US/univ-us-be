package com.univus.app.member.service;

import com.univus.app.member.dto.AccountRecoveryDto;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountRecoveryServiceImpl implements AccountRecoveryService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private static final String OTP_KEY_PREFIX = "auth:recovery:otp:";
    private static final String OTP_ATTEMPT_KEY_PREFIX = "auth:recovery:otp-attempt:";
    private static final String OTP_COOLDOWN_KEY_PREFIX = "auth:recovery:otp-cooldown:";
    private static final String RESET_TOKEN_KEY_PREFIX = "auth:recovery:reset:";

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RecoveryVerificationProvider verificationProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AccountRecoveryDto.VerificationChallengeResponse sendIdRecoveryCode(
            AccountRecoveryDto.IdentityRequest request
    ) {
        RecoveryIdentity identity = normalizeIdentity(request);
        List<MemberDto> members = memberMapper.findByRecoveryIdentity(
                identity.memberName(),
                identity.phoneNumber(),
                identity.birth()
        );
        assertMemberExists(members.isEmpty());
        return issueChallenge(RecoveryType.ID, identity.phoneText());
    }

    @Override
    public AccountRecoveryDto.IdVerifyResponse verifyIdRecoveryCode(
            AccountRecoveryDto.IdVerifyRequest request
    ) {
        RecoveryIdentity identity = normalizeIdentity(request);
        verifyChallenge(RecoveryType.ID, identity.phoneText());

        List<MemberDto> members = memberMapper.findByRecoveryIdentity(
                identity.memberName(),
                identity.phoneNumber(),
                identity.birth()
        );
        assertMemberExists(members.isEmpty());

        AccountRecoveryDto.IdVerifyResponse response = new AccountRecoveryDto.IdVerifyResponse();
        response.setAccounts(members.stream().map(member -> {
            AccountRecoveryDto.LoginIdItem item = new AccountRecoveryDto.LoginIdItem();
            item.setLoginId(member.getLoginId());
            item.setUnivName(member.getUnivName());
            return item;
        }).toList());
        return response;
    }

    @Override
    public AccountRecoveryDto.VerificationChallengeResponse sendPasswordRecoveryCode(
            AccountRecoveryDto.PasswordIdentityRequest request
    ) {
        RecoveryIdentity identity = normalizeIdentity(request);
        MemberDto member = memberMapper.findByLoginIdAndRecoveryIdentity(
                normalizeLoginId(request.getLoginId()),
                identity.memberName(),
                identity.phoneNumber(),
                identity.birth()
        );
        assertMemberExists(member == null);
        return issueChallenge(RecoveryType.PASSWORD, identity.phoneText());
    }

    @Override
    public AccountRecoveryDto.PasswordVerifyResponse verifyPasswordRecoveryCode(
            AccountRecoveryDto.PasswordVerifyRequest request
    ) {
        RecoveryIdentity identity = normalizeIdentity(request);
        verifyChallenge(RecoveryType.PASSWORD, identity.phoneText());

        MemberDto member = memberMapper.findByLoginIdAndRecoveryIdentity(
                normalizeLoginId(request.getLoginId()),
                identity.memberName(),
                identity.phoneNumber(),
                identity.birth()
        );
        assertMemberExists(member == null);

        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                RESET_TOKEN_KEY_PREFIX + resetToken,
                String.valueOf(member.getMemberId()),
                RESET_TOKEN_TTL
        );

        AccountRecoveryDto.PasswordVerifyResponse response = new AccountRecoveryDto.PasswordVerifyResponse();
        response.setResetToken(resetToken);
        return response;
    }

    @Transactional
    @Override
    public void resetPassword(AccountRecoveryDto.ResetPasswordRequest request) {
        String password = request.getPassword() == null ? "" : request.getPassword().trim();
        if (password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호를 입력해주세요.");
        }

        String resetToken = request.getResetToken() == null ? "" : request.getResetToken().trim();
        Object memberIdValue = redisTemplate.opsForValue().getAndDelete(RESET_TOKEN_KEY_PREFIX + resetToken);
        if (memberIdValue == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호 재설정 인증이 만료되었습니다.");
        }

        Long memberId = Long.valueOf(String.valueOf(memberIdValue));
        int updated = memberMapper.updatePassword(memberId, passwordEncoder.encode(password));
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다.");
        }

        refreshTokenService.deleteMemberSessions(memberId);
    }

    private AccountRecoveryDto.VerificationChallengeResponse issueChallenge(
            RecoveryType type,
            String phoneNumber
    ) {
        String challengeKey = challengeKey(type, phoneNumber);
        Boolean created = redisTemplate.opsForValue().setIfAbsent(
                OTP_COOLDOWN_KEY_PREFIX + challengeKey,
                "1",
                RESEND_COOLDOWN
        );
        if (!Boolean.TRUE.equals(created)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "인증번호는 60초 후에 다시 요청할 수 있습니다."
            );
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String otpKey = OTP_KEY_PREFIX + challengeKey;

        redisTemplate.opsForValue().set(otpKey, code, OTP_TTL);
        redisTemplate.delete(OTP_ATTEMPT_KEY_PREFIX + challengeKey);

        RecoveryVerificationProvider.VerificationInstruction instruction = verificationProvider.issueChallenge(
                phoneNumber,
                code
        );
        AccountRecoveryDto.VerificationChallengeResponse response = new AccountRecoveryDto.VerificationChallengeResponse();
        response.setRecipientNumber(instruction.recipientNumber());
        response.setMessageText(instruction.messageText());
        response.setExpiresInSeconds(OTP_TTL.toSeconds());
        return response;
    }

    private void verifyChallenge(RecoveryType type, String phoneNumber) {
        String challengeKey = challengeKey(type, phoneNumber);
        String otpKey = OTP_KEY_PREFIX + challengeKey;
        Object storedCode = redisTemplate.opsForValue().get(otpKey);
        if (storedCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 존재하지 않습니다.");
        }

        if (verificationProvider.verify(phoneNumber, String.valueOf(storedCode))) {
            deleteOtp(challengeKey);
            return;
        }

        Long attempts = redisTemplate.opsForValue().increment(OTP_ATTEMPT_KEY_PREFIX + challengeKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(OTP_ATTEMPT_KEY_PREFIX + challengeKey, OTP_TTL);
        }
        if (attempts != null && attempts >= MAX_VERIFY_ATTEMPTS) {
            deleteOtp(challengeKey);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호 입력 횟수를 초과했습니다.");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.");
    }

    private void deleteOtp(String challengeKey) {
        redisTemplate.delete(List.of(
                OTP_KEY_PREFIX + challengeKey,
                OTP_ATTEMPT_KEY_PREFIX + challengeKey
        ));
    }

    private RecoveryIdentity normalizeIdentity(AccountRecoveryDto.IdentityRequest request) {
        String memberName = request.getMemberName() == null ? "" : request.getMemberName().trim();
        String phoneText = request.getPhoneNumber() == null
                ? ""
                : request.getPhoneNumber().replaceAll("\\D", "");
        String birth = request.getBirth() == null ? "" : request.getBirth().trim();

        if (memberName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름을 입력해주세요.");
        }
        if (!phoneText.matches("^010\\d{8}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "휴대폰번호는 010으로 시작하는 숫자 11자리여야 합니다.");
        }
        if (!birth.matches("^\\d{8}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "생년월일은 숫자 8자리여야 합니다.");
        }

        return new RecoveryIdentity(memberName, Long.parseLong(phoneText), phoneText, birth);
    }

    private String normalizeLoginId(String loginId) {
        String normalized = loginId == null ? "" : loginId.trim();
        if (!normalized.matches("^\\d+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그인 ID는 숫자만 입력해주세요.");
        }
        return normalized;
    }

    private void assertMemberExists(boolean missing) {
        if (missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 번호가 존재하지 않습니다.");
        }
    }

    private String challengeKey(RecoveryType type, String phoneNumber) {
        return sha256(type.name() + ":" + phoneNumber);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private enum RecoveryType {
        ID,
        PASSWORD
    }

    private record RecoveryIdentity(String memberName, Long phoneNumber, String phoneText, String birth) {
    }
}
