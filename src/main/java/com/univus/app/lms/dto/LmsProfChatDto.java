package com.univus.app.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public final class LmsProfChatDto {

    private LmsProfChatDto() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRoomResDto {
        private Long roomId;
        private Long lecId;
        private Long studentLmsPrfId;
        private Long professorLmsPrfId;
        private String studentName;
        private String studentNo;
        private String courseName;
        private Integer lecSection;
        private String lastMessage;
        private String lastAt;
        private Integer unread;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessageResDto {
        private Long id;
        private Long roomId;
        private Long senderLmsPrfId;
        private String sender;
        private String text;
        private String sentAt;
        private Boolean read;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatThreadResDto {
        private Long roomId;
        private String dateLabel;
        private List<ChatMessageResDto> messages;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendMessageReqDto {
        private String messageText;
    }
}
