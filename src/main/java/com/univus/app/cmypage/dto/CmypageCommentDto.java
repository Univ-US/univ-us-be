package com.univus.app.cmypage.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CmypageCommentDto {
    private Long commentId;
    private String content;
    private String postTitle;
    private Long boardId;
    private String boardName;
    private LocalDateTime createdAt;
}
