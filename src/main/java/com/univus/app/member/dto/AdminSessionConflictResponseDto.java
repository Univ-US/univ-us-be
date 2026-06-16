package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSessionConflictResponseDto {

    private boolean success = false;
    private String code = "ADMIN_SESSION_CONFLICT";
    private String message = "An administrator session is already active.";
    private AdminSessionInfoDto session;
}
