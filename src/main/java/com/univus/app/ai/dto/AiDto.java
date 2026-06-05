package com.univus.app.ai.dto;

import lombok.Data;

@Data
public class AiDto {
    private Long chatId;
    private Long memberId;
    private String message;
    private String response;
    private String aiCreatedAt;
}