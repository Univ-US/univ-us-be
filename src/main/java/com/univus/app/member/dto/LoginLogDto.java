package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginLogDto {

    private Long logId;
    private Long memberId;
    private String result;
    private String failReason;

}
