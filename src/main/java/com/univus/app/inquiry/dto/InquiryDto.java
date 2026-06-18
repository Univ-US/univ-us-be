package com.univus.app.inquiry.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class InquiryDto {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_CLOSED = "CLOSED";

    @Data
    public static class Room {
        private Long roomId;
        private Long univId;
        private String univName;
        private String title;
        private String category;
        private String status;
        private String createdAt;
        private String lastAt;
        private String lastMessage;
        private Long adminMemberId;
        private String adminName;
        private Integer unreadCount;
    }

    @Data
    public static class Thread {
        private Long roomId;
        private Long univId;
        private String univName;
        private String title;
        private String category;
        private String status;
        private String createdAt;
        private List<Message> messages = new ArrayList<>();
    }

    @Data
    public static class Message {
        private Long messageId;
        private Long roomId;
        private Long memberId;
        private String senderName;
        private String senderRole;
        private String content;
        private String sentAt;
        private Boolean read;
        private List<Attachment> attachments = new ArrayList<>();
    }

    @Data
    public static class Attachment {
        private Long attachmentId;
        private Long messageId;
        private String originalName;
        private String saveName;
        private String filePath;
        private Long fileSize;
        private String fileType;
        private String createdAt;
        private String downloadUrl;
    }

    @Data
    public static class CreateRequest {
        private String title;
        private String category;
        private String messageText;
    }

    @Data
    public static class SendMessageRequest {
        private String messageText;
    }
}
