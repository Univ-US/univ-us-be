package com.univus.app.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenResponseDto {

    @JsonIgnore
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    @JsonIgnore
    private String tokenType;
    private Long memberId;
    private String role;

}
