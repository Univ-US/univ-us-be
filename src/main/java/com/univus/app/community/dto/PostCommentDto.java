package com.univus.app.community.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCommentDto {

    // ── POST_COMMENT 테이블 ───────────────────────
    private Long commentId;            // COMMENT_ID
    private Long memberId;             // MEMBER_ID
    private Long postId;               // POST_ID
    private Long parentId;             // PARENT_ID (NULL = 최상위 댓글)
    private String content;            // CONTENT VARCHAR2(2000)
    private int isAnonymous;           // IS_ANONYMOUS NUMBER(1)
    private int isDeleted;             // IS_DELETED NUMBER(1)
    private LocalDateTime createdAt;   // CREATED_AT

    // ── MEMBER JOIN (프론트 필드명 기준) ──────────
    private String authorName;         // MEMBER.MEMBER_NAME
    private String authorNickname;     // MEMBER.COMMUNITY_NICKNAME

    // ── 댓글 좋아요 (DB 없음, 0 고정) ───────────
    private int likeCount;

    // ── 요청 파라미터용 ───────────────────────────
    private int page;
    private int size;
}