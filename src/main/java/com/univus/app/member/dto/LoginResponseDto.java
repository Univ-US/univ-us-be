package com.univus.app.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoginResponseDto {

    @JsonIgnore
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    @JsonIgnore
    private String tokenType;
    private Long memberId;
    private String role;
    private Long univId;
    private Long phoneNumber;
    private LocalDateTime createdAt;
    private String univName;
    private String memberName;
    private String communityNickname;
    private String status;

}
