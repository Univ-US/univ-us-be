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
        private Long univId;
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
        private Integer reportCount;
        private Integer isBlind;
        private Boolean hasUnreadTradeChat;

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
        private Long productId;
        private Long memberId;
        private Long univId;
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
        private Long univId;
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
        private Long univId;
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

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductReportDto {
        private Long reportId;
        private Long memberId;
        private Long productId;
        private String reason;
        private String detail;
        private LocalDateTime pressedAt;
    }

    // ── 8. 목록 조회 파라미터 ─────────────────────────────────
    @Getter @Setter
    @NoArgsConstructor
    public static class ProductSearchDto {
        private String keyword;
        private String category;
        private String productStatus;
        private Long viewerMemberId;
        private Long univId;
        private Integer page;
        private Integer size;
    }
    @Getter @Setter
    @NoArgsConstructor
    public static class PaymentConfigDto {
        private String storeId;
        private String kgInicisChannelKey;
        private String kakaoPayChannelKey;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class PaymentCompleteDto {
        private Long productId;
        private String paymentId;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class ChatPaymentCompleteDto {
        private Long roomId;
        private String paymentId;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TradeDto {
        private Long tradeId;
        private Long productId;
        private Long sellerId;
        private Long buyerId;
        private String tradeStatus;
        private Long price;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDto {
        private Long paymentId;
        private Long tradeId;
        private String impUid;
        private String merchantUid;
        private Long amount;
        private String status;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentResultDto {
        private Long tradeId;
        private Long paymentId;
        private String productStatus;
        private String paymentStatus;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TradeChatRoomDto {
        private Long roomId;
        private Long productId;
        private Long sellerId;
        private Long buyerId;
        private Long negotiatedPrice;
        private String status;
        private LocalDateTime createdAt;
        private String productName;
        private String productStatus;
        private String sellerName;
        private String buyerName;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private Integer unreadCount;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class TradeChatRoomRequestDto {
        private Long productId;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TradeChatMessageDto {
        private Long messageId;
        private Long roomId;
        private Long senderId;
        private String content;
        private Integer isRead;
        private LocalDateTime sendAt;
        private String senderName;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class TradeChatMessageRequestDto {
        private String content;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class TradeChatPriceRequestDto {
        private Long negotiatedPrice;
    }
}
