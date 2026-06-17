package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSessionInfoDto {

    private String loginAt;
    private String ipAddress;
    private String device;
}
