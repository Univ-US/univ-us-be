package com.univus.app.community.mapper;

import com.univus.app.community.dto.MarketDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MarketMapper {

    // ── 상품 ──────────────────────────────────────────────────

    // 상품 목록 조회 (검색/필터/페이징 포함)
    List<MarketDto.ProductDto> selectProductList(MarketDto.ProductSearchDto searchDto);

    // 상품 전체 개수 (페이징용)
    int selectProductCount(MarketDto.ProductSearchDto searchDto);

    // 상품 상세 조회
    MarketDto.ProductDto selectProductDetail(Long productId);

    // 상품 등록
    int insertProduct(MarketDto.ProductCreateDto createDto);

    // 상품 수정
    int updateProduct(MarketDto.ProductUpdateDto updateDto);

    // 상품 삭제 (소프트 딜리트)
    int deleteProduct(Long productId);

    // 조회수 증가
    int updateViewCount(Long productId);

    // ── 상품 이미지 ───────────────────────────────────────────

    // 이미지 등록 (단건)
    int insertProductImage(MarketDto.ProductImageDto imageDto);

    // 이미지 목록 조회
    List<MarketDto.ProductImageDto> selectProductImageList(Long productId);

    // 이미지 전체 삭제 (상품 수정 시 기존 이미지 교체용)
    int deleteProductImageAll(Long productId);

    int deleteProductImagesExcept(@Param("productId") Long productId, @Param("keepImageIds") List<Long> keepImageIds);

    // ── 댓글 ──────────────────────────────────────────────────

    // 댓글 목록 조회 (부모 댓글만)
    List<MarketDto.ProductCommentDto> selectProductCommentList(Long productId);

    MarketDto.ProductCommentDto selectProductCommentById(@Param("commentId") Long commentId);

    // 대댓글 목록 조회
    List<MarketDto.ProductCommentDto> selectProductReplyList(Long parentId);

    // 댓글 등록
    int insertProductComment(MarketDto.ProductCommentCreateDto createDto);

    // 댓글 삭제 (소프트 딜리트)
    int deleteProductComment(Long commentId);

    // ── 찜(좋아요) ────────────────────────────────────────────

    // 찜 추가
    int insertProductLike(MarketDto.ProductLikeDto likeDto);

    // 찜 취소
    int deleteProductLike(MarketDto.ProductLikeDto likeDto);

    // 찜 여부 확인
    int selectProductLikeCount(MarketDto.ProductLikeDto likeDto);

    int selectProductReportCount(MarketDto.ProductReportDto reportDto);

    int selectProductTotalReportCount(Long productId);

    int insertProductReport(MarketDto.ProductReportDto reportDto);

    int updateProductReportStatus(Long productId);

    // 내 찜 목록
    List<MarketDto.ProductDto> selectMyLikeList(
            @Param("memberId") Long memberId,
            @Param("univId") Long univId);

    int insertTrade(MarketDto.TradeDto tradeDto);

    int insertPayment(MarketDto.PaymentDto paymentDto);

    int updateProductStatus(@Param("productId") Long productId, @Param("productStatus") String productStatus);

    MarketDto.TradeChatRoomDto selectTradeChatRoomByBuyer(
            @Param("productId") Long productId,
            @Param("buyerId") Long buyerId);

    int insertTradeChatRoom(MarketDto.TradeChatRoomDto roomDto);

    MarketDto.TradeChatRoomDto selectTradeChatRoomForMember(
            @Param("roomId") Long roomId,
            @Param("memberId") Long memberId);

    List<MarketDto.TradeChatRoomDto> selectTradeChatRoomsForMember(@Param("memberId") Long memberId);

    int selectUnreadTradeChatMessageCountByProduct(
            @Param("productId") Long productId,
            @Param("memberId") Long memberId);

    List<MarketDto.TradeChatMessageDto> selectTradeChatMessages(@Param("roomId") Long roomId);

    int insertTradeChatMessage(MarketDto.TradeChatMessageDto messageDto);

    MarketDto.TradeChatMessageDto selectTradeChatMessage(@Param("messageId") Long messageId);

    int markTradeChatMessagesAsRead(
            @Param("roomId") Long roomId,
            @Param("memberId") Long memberId);

    int updateTradeChatNegotiatedPrice(@Param("roomId") Long roomId, @Param("negotiatedPrice") Long negotiatedPrice);

    int updateTradeChatStatus(@Param("roomId") Long roomId, @Param("status") String status);

    List<MarketDto.TradeChatRoomDto> selectActiveTradeChatRoomsByProduct(@Param("productId") Long productId);

    int updateActiveTradeChatStatusByProduct(@Param("productId") Long productId, @Param("status") String status);

    int updateOtherActiveTradeChatStatusByProduct(
            @Param("productId") Long productId,
            @Param("excludeRoomId") Long excludeRoomId,
            @Param("status") String status);

    int selectActiveTradeChatRoomCountByProduct(@Param("productId") Long productId);

    int deleteTradeChatMessages(@Param("roomId") Long roomId);

    int deleteTradeChatRoom(@Param("roomId") Long roomId);
}
