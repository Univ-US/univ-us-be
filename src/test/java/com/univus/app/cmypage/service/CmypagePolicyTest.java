package com.univus.app.cmypage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.cmypage.dto.CmypageProfileUpdateDto;
import com.univus.app.exception.ConflictException;
import com.univus.app.exception.InvalidRequestException;

class CmypagePolicyTest {

    private final CmypagePolicy cmypagePolicy =
            new CmypagePolicyImpl();

    @Test
    @DisplayName("닉네임은 공백을 제거한 뒤 반환한다")
    void getValidatedNicknameTrimsValue() {
        CmypageProfileUpdateDto request =
                new CmypageProfileUpdateDto();
        request.setCommunityNickname("  유니버스  ");

        String nickname =
                cmypagePolicy.getValidatedNickname(request);

        assertEquals("유니버스", nickname);
    }

    @Test
    @DisplayName("중복 닉네임은 충돌 예외로 처리한다")
    void requireAvailableNicknameRejectsDuplicate() {
        assertThrows(
                ConflictException.class,
                () -> cmypagePolicy.requireAvailableNickname(1));
    }

    @Test
    @DisplayName("잘못된 거래 역할은 전체 조회로 변경한다")
    void normalizeTradeRoleUsesAllForUnknownRole() {
        assertEquals(
                "ALL",
                cmypagePolicy.normalizeTradeRole("unknown"));
    }

    @Test
    @DisplayName("닉네임 요청이 없으면 잘못된 요청 예외로 처리한다")
    void getValidatedNicknameRequiresRequest() {
        assertThrows(
                InvalidRequestException.class,
                () -> cmypagePolicy.getValidatedNickname(null));
    }
}
