package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginSessionDto {

    private Long sessionId;
    private Long memberId;
    private String refreshToken;
    private String ipAddress;

}
