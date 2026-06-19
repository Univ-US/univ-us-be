package com.univus.app.cmypage.service;

import org.springframework.stereotype.Service;

import com.univus.app.cmypage.dto.CmypageProfileDto;
import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;
import com.univus.app.exception.ConflictException;
import com.univus.app.exception.InvalidRequestException;

@Service
public class CmypagePolicyImpl implements CmypagePolicy {

    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;
    private static final String TRADE_ROLE_ALL = "ALL";
    private static final String TRADE_ROLE_SELLER = "SELLER";
    private static final String TRADE_ROLE_BUYER = "BUYER";

    @Override
    public CmypageProfileDto requireProfile(CmypageProfileDto profile) {
        if (profile == null) {
            throw new InvalidRequestException("회원 정보를 찾을 수 없습니다.");
        }
        return profile;
    }

    @Override
    public String getValidatedNickname(CmypageProfileUpdateDto request) {
        if (request == null) {
            throw new InvalidRequestException("요청 본문이 필요합니다.");
        }

        String nickname = request.getCommunityNickname();
        if (nickname == null) {
            throw new InvalidRequestException("닉네임을 입력해주세요.");
        }

        nickname = nickname.trim();
        if (nickname.length() < MIN_NICKNAME_LENGTH
                || nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new InvalidRequestException(
                    "닉네임은 2자 이상 20자 이하로 입력해주세요.");
        }
        return nickname;
    }

    @Override
    public void requireAvailableNickname(int duplicateCount) {
        if (duplicateCount > 0) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }
    }

    @Override
    public void requireDepartmentAvailable(
            boolean hasMemberDetail,
            Long defaultDeptId) {
        if (!hasMemberDetail && defaultDeptId == null) {
            throw new ConflictException(
                    "소속 대학의 학과 정보가 없어 커뮤니티 닉네임을 설정할 수 없습니다.");
        }
    }

    @Override
    public void requireProfileUpdated(int updated) {
        if (updated <= 0) {
            throw new InvalidRequestException(
                    "회원 상세 정보를 찾을 수 없습니다.");
        }
    }

    @Override
    public String normalizeTradeRole(String role) {
        if (role == null) {
            return TRADE_ROLE_ALL;
        }

        String normalizedRole = role.trim().toUpperCase();
        if (TRADE_ROLE_SELLER.equals(normalizedRole)) {
            return TRADE_ROLE_SELLER;
        }
        if (TRADE_ROLE_BUYER.equals(normalizedRole)) {
            return TRADE_ROLE_BUYER;
        }
        return TRADE_ROLE_ALL;
    }
}
