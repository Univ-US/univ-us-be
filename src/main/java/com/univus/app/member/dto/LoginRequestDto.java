package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {

    private String loginId;
    private Long univId;
    private String password;

}
