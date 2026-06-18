package com.univus.app.community.service;

import com.univus.app.community.dto.MarketDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MarketService {

    List<MarketDto.ProductDto> getProductList(MarketDto.ProductSearchDto searchDto, Long memberId);

    int getProductCount(MarketDto.ProductSearchDto searchDto, Long memberId);

    MarketDto.ProductDto getProductDetail(Long productId, Long memberId);

    MarketDto.ProductDto findProductById(Long productId);

    int createProduct(MarketDto.ProductCreateDto createDto);

    int updateProduct(MarketDto.ProductUpdateDto updateDto);

    MarketDto.ProductDto completeFreeProduct(Long productId, Long memberId);

    int deleteProduct(Long productId);

    int addProductImage(MarketDto.ProductImageDto imageDto);

    List<MarketDto.ProductImageDto> uploadProductImages(Long productId, List<MultipartFile> images);

    void replaceProductImages(Long productId, List<MarketDto.ProductImageDto> images);

    List<MarketDto.ProductImageDto> replaceProductImagesWithFiles(Long productId, List<MultipartFile> images);

    List<MarketDto.ProductImageDto> updateProductImages(Long productId, List<Long> keepImageIds, List<MultipartFile> images);

    List<MarketDto.ProductImageDto> getProductImageList(Long productId, Long memberId);

    List<MarketDto.ProductCommentDto> getProductCommentList(Long productId, Long memberId);

    MarketDto.ProductCommentDto findProductCommentById(Long commentId);

    int createProductComment(MarketDto.ProductCommentCreateDto createDto);

    int deleteProductComment(Long commentId);

    boolean toggleProductLike(MarketDto.ProductLikeDto likeDto);

    boolean isProductLiked(MarketDto.ProductLikeDto likeDto);

    int getProductLikeCount(Long productId, Long memberId);

    Map<String, Object> reportProduct(MarketDto.ProductReportDto reportDto);

    boolean isProductReported(Long productId, Long memberId);

    List<MarketDto.TradeChatRoomDto> getTradeChatRooms(Long memberId);

    MarketDto.TradeChatRoomDto createOrGetTradeChatRoom(
            Long memberId,
            MarketDto.TradeChatRoomRequestDto request);

    List<MarketDto.TradeChatMessageDto> getTradeChatMessages(Long memberId, Long roomId);

    MarketDto.TradeChatMessageDto sendTradeChatMessage(
            Long memberId,
            Long roomId,
            MarketDto.TradeChatMessageRequestDto request);

    MarketDto.TradeChatRoomDto updateTradeChatNegotiatedPrice(
            Long memberId,
            Long roomId,
            MarketDto.TradeChatPriceRequestDto request);

    MarketDto.TradeChatRoomDto completeFreeTradeChat(Long memberId, Long roomId);

    MarketDto.TradeChatRoomDto closeTradeChatRoom(Long memberId, Long roomId);

    Map<String, Object> deleteTradeChatRoom(Long memberId, Long roomId);

    List<MarketDto.ProductDto> getMyLikeList(Long memberId);

    int markTradeChatMessagesAsRead(Long memberId, Long roomId);

    MarketDto.PaymentConfigDto getPaymentConfig();

    MarketDto.PaymentResultDto completePayment(MarketDto.PaymentCompleteDto completeDto, Long buyerId);

    MarketDto.PaymentResultDto completeChatPayment(
            MarketDto.ChatPaymentCompleteDto completeDto,
            Long buyerId);

    void assertProductAccessible(Long productId, Long memberId);
}
