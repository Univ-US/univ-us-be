package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoginResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long memberId;
    private String role;
    private Long univId;
    private Long phoneNumber;
    private LocalDateTime createdAt;
    private String univName;
    private String memberName;
    private String communityNickname;

}
