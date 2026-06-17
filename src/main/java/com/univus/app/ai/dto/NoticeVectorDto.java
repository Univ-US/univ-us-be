package com.univus.app.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeVectorDto {
    private Long noticeId;
    private Long univId;
    private String title;
    private String content;
    private String target;
    private LocalDateTime postedAt;
    private double similarity;
}
