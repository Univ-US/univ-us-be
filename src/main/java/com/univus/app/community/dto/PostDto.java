package com.univus.app.community.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {

    // ── POST 테이블 ───────────────────────────────
    private Long postId;
    private Long memberId;
    private Long boardId;
    private String title;
    private String content;          // CLOB
    private String category;         // VARCHAR2(20) NULL
    private int viewCount;
    private int isBlind;
    private int reportCount;
    private int isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── MEMBER JOIN ───────────────────────────────
    private String authorName;       // MEMBER.MEMBER_NAME
    private String authorNickname;   // MEMBER.COMMUNITY_NICKNAME

    // ── BOARD_TYPE JOIN ───────────────────────────
    private String boardName;        // BOARD_TYPE.BOARD_NAME
    private int isAnonymous;         // BOARD_TYPE.IS_ANONYMOUS

    // ── COUNT JOIN ────────────────────────────────
    private int likeCount;           // POST_LIKE COUNT
    private int commentCount;        // POST_COMMENT COUNT
    private List<PostImageDto> images;

    // ── 요청 파라미터용 ───────────────────────────
    private int page;
    private int size;
    private String keyword;

    // ── 신고 ──────────────────────────────────────
    private String reason;           // POST_REPORT.REASON
    private String detail;           // POST_REPORT.DETAIL
}
