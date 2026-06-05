package com.univus.app.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class MarketDto {

    // ── 1. 상품 목록/상세 조회 응답 ─────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductDto {
        private Long productId;
        private Long memberId;
        private String productName;
        private Long price;
        private String description;
        private Integer viewCount;
        private Integer isDeleted;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String productStatus;   // 'SALE' | 'RESERVE' | 'DONE'
        private String place;
        private String category;        // '교재' | '전자기기' | '생활용품' | '기타'

        // MEMBER JOIN
        private String sellerName;
        private String sellerNickname;

        // COUNT JOIN
        private Integer likeCount;
        private Integer chatCount;

        // PRODUCT_IMAGE JOIN
        private List<ProductImageDto> images;
    }

    // ── 2. 상품 이미지 ────────────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductImageDto {
        private Long imageId;
        private Long productId;
        private String imageUrl;
        private Integer imageSort;
        private LocalDateTime createdAt;
    }

    // ── 3. 상품 등록 요청 ─────────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    public static class ProductCreateDto {
        private Long memberId;
        private String productName;
        private Long price;
        private String description;
        private String place;
        private String category;
        private String productStatus;
    }

    // ── 4. 상품 수정 요청 ─────────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    public static class ProductUpdateDto {
        private Long productId;
        private String productName;
        private Long price;
        private String description;
        private String place;
        private String category;
        private String productStatus;
    }

    // ── 5. 상품 댓글 조회 응답 ────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductCommentDto {
        private Long commentId;
        private Long memberId;
        private Long productId;
        private Long parentId;
        private String content;
        private Integer isAnonymous;
        private Integer isDeleted;
        private LocalDateTime createdAt;

        // MEMBER JOIN
        private String authorName;
        private String authorNickname;
        private Integer likeCount;

        // 대댓글 목록
        private List<ProductCommentDto> replies;
    }

    // ── 6. 댓글 작성 요청 ─────────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    public static class ProductCommentCreateDto {
        private Long memberId;
        private Long productId;
        private Long parentId;
        private String content;
        private Integer isAnonymous;
    }

    // ── 7. 찜(좋아요) ──────────────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductLikeDto {
        private Long likeId;
        private Long memberId;
        private Long productId;
        private LocalDateTime pressedAt;
    }

    // ── 8. 목록 조회 파라미터 ─────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    public static class ProductSearchDto {
        private String keyword;
        private String category;
        private String productStatus;
        private int page;
        private int size;
    }
}