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
    private Long viewerMemberId;
    private Long univId;
    private Long boardId;
    private String title;
    private String content;          // CLOB
    private String category;         // VARCHAR2(20) NULL
    private Integer viewCount;
    private Integer isBlind;
    private Integer reportCount;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── MEMBER JOIN ───────────────────────────────
    private String authorName;       // MEMBER.MEMBER_NAME
    private String authorNickname;   // MEMBER.COMMUNITY_NICKNAME
    private String authorRole;       // MEMBER.ROLE
    private Integer isAdminNotice;   // 관리자 작성 자유/익명 게시글 여부
    private Integer isPinned;        // 목록 상단 고정 여부

    // ── BOARD_TYPE JOIN ───────────────────────────
    private String boardName;        // BOARD_TYPE.BOARD_NAME
    private Integer isAnonymous;     // BOARD_TYPE.IS_ANONYMOUS

    // ── COUNT JOIN ────────────────────────────────
    private Integer likeCount;       // POST_LIKE COUNT
    private Integer commentCount;    // POST_COMMENT COUNT
    private List<PostImageDto> images;

    // ── 요청 파라미터용 ───────────────────────────
    private Integer page;
    private Integer size;
    private String keyword;

    // ── 신고 ──────────────────────────────────────
    private String reason;           // POST_REPORT.REASON
    private String detail;           // POST_REPORT.DETAIL
}
