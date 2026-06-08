package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long memberId;
    private String role;
    private Long univId;

}
