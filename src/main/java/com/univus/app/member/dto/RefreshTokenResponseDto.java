package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenResponseDto {

    private String accessToken;
    private String tokenType;
    private Long memberId;
    private String role;

}