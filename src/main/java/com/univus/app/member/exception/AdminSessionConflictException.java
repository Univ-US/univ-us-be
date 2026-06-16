package com.univus.app.member.exception;

import com.univus.app.member.dto.AdminSessionConflictResponseDto;
import com.univus.app.member.dto.AdminSessionInfoDto;
import lombok.Getter;

@Getter
public class AdminSessionConflictException extends RuntimeException {

    private final AdminSessionConflictResponseDto response;

    public AdminSessionConflictException(AdminSessionInfoDto session) {
        super("An administrator session is already active.");

        AdminSessionConflictResponseDto response = new AdminSessionConflictResponseDto();
        response.setSession(session);
        this.response = response;
    }
}
