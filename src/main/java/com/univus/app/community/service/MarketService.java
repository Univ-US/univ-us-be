package com.univus.app.community.service;

import com.univus.app.common.StorageService;
import com.univus.app.community.dto.MarketDto;
import com.univus.app.community.mapper.MarketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.univus.app.community.service.CommunityAccessService.CommunityAccessScope;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketMapper marketMapper;
    private final RestTemplate restTemplate;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CommunityAccessService communityAccessService;

    @Value("${file.upload-root:${user.home}/univus/uploads}")
    private String uploadRoot;

    @Value("${portone.trade.store-id:}")
    private String portoneTradeStoreId;

    @Value("${portone.trade.kg-inicis-channel-key:}")
    private String portoneKgInicisChannelKey;

    @Value("${portone.trade.kakao-pay-channel-key:}")
    private String portoneKakaoPayChannelKey;

    @Value("${portone.trade.api-secret:}")
    private String portoneTradeApiSecret;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_IMAGE_SIZE = 30L * 1024 * 1024;
    private static final String PRODUCT_IMAGE_SUBDIR = "community" + File.separator + "market";
    private static final String PRODUCT_IMAGE_URL_PREFIX = "/uploads/community/market/";
    private static final String DEFAULT_TRADE_CHAT_STATUS = "ACTIVE";
    private static final String DONE_TRADE_CHAT_STATUS = "DONE";
    private static final String CLOSED_TRADE_CHAT_STATUS = "CLOSED";
    private static final String MARKET_CHAT_TOPIC_PREFIX = "/sub/market-chats/";
    private static final int MAX_CHAT_MESSAGE_LENGTH = 2000;

    // ── 상품 ──────────────────────────────────────────────────

    // 상품 목록 조회
    public List<MarketDto.ProductDto> getProductList(MarketDto.ProductSearchDto searchDto, Long memberId) {
        applySchoolScope(searchDto, memberId);
        List<MarketDto.ProductDto> products = marketMapper.selectProductList(searchDto);
        for (MarketDto.ProductDto product : products) {
            product.setImages(marketMapper.selectProductImageList(product.getProductId()));
        }
        return products;
    }

    // 상품 전체 개수 (페이징용)
    public int getProductCount(MarketDto.ProductSearchDto searchDto, Long memberId) {
        applySchoolScope(searchDto, memberId);
        return marketMapper.selectProductCount(searchDto);
    }

    // 상품 상세 조회 + 조회수 증가
    @Transactional
    public MarketDto.ProductDto getProductDetail(Long productId, Long memberId) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            return null;
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(memberId));
        marketMapper.updateViewCount(productId);
        product = marketMapper.selectProductDetail(productId);
        if (product != null) {
            product.setImages(marketMapper.selectProductImageList(productId));
        }
        return product;
    }

    public MarketDto.ProductDto findProductById(Long productId) {
        return marketMapper.selectProductDetail(productId);
    }

    // 상품 등록
    @Transactional
    public int createProduct(MarketDto.ProductCreateDto createDto) {
        CommunityAccessScope scope = communityAccessService.getScope(createDto.getMemberId());
        if (scope.isSuperAdmin()) {
            if (createDto.getUnivId() == null) {
                throw badRequest("University id is required.");
            }
        } else {
            createDto.setUnivId(scope.getUnivId());
        }
        createDto.setProductStatus("SALE");
        return marketMapper.insertProduct(createDto);
    }

    // 상품 수정
    @Transactional
    public int updateProduct(MarketDto.ProductUpdateDto updateDto) {
        validateEditableProductStatus(updateDto.getProductStatus());
        MarketDto.ProductDto product = marketMapper.selectProductDetail(updateDto.getProductId());
        if (product != null && "DONE".equals(product.getProductStatus())) {
            throw conflict("Completed product cannot be edited.");
        }
        return marketMapper.updateProduct(updateDto);
    }

    // 무료 나눔 완료
    @Transactional
    public MarketDto.ProductDto completeFreeProduct(Long productId, Long memberId) {
        validateMember(memberId);
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null || product.getIsDeleted() == 1) {
            throw notFound("Product not found.");
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(memberId));
        if (!product.getMemberId().equals(memberId)) {
            throw forbidden("Only seller can complete free sharing.");
        }
        if (product.getPrice() == null || product.getPrice() > 0) {
            throw conflict("Only free sharing products can be completed without payment.");
        }
        if ("DONE".equals(product.getProductStatus())) {
            return product;
        }
        if (isBlindProduct(product)) {
            throw conflict("Blind product cannot be completed.");
        }

        marketMapper.updateProductStatus(productId, "DONE");
        marketMapper.updateActiveTradeChatStatusByProduct(productId, DONE_TRADE_CHAT_STATUS);
        return marketMapper.selectProductDetail(productId);
    }

    public int deleteProduct(Long productId) {
        int rows = marketMapper.deleteProduct(productId);
        if (rows > 0) {
            closeActiveTradeChatRoomsByProduct(productId);
        }
        return rows;
    }

    // ── 이미지 ────────────────────────────────────────────────

    // 이미지 등록
    @Transactional
    public int addProductImage(MarketDto.ProductImageDto imageDto) {
        return marketMapper.insertProductImage(imageDto);
    }

    @Transactional
    public List<MarketDto.ProductImageDto> uploadProductImages(Long productId, List<MultipartFile> images) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            throw notFound("Product not found.");
        }
        if (images == null || images.isEmpty()) {
            return marketMapper.selectProductImageList(productId);
        }

        int nextSort = marketMapper.selectProductImageList(productId).size() + 1;
        String directoryPath = uploadRoot + File.separator + PRODUCT_IMAGE_SUBDIR + File.separator + productId;
        String urlPrefix = PRODUCT_IMAGE_URL_PREFIX + productId + "/";

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            validateProductImage(image);
            String savedFilename = storageService.uploadFileToServer(image, directoryPath);
            if (savedFilename == null) {
                continue;
            }

            MarketDto.ProductImageDto imageDto = MarketDto.ProductImageDto.builder()
                    .productId(productId)
                    .imageUrl(urlPrefix + savedFilename)
                    .imageSort(nextSort++)
                    .build();
            marketMapper.insertProductImage(imageDto);
        }

        return marketMapper.selectProductImageList(productId);
    }

    // 이미지 전체 교체 (수정 시 기존 삭제 후 재등록)
    @Transactional
    public void replaceProductImages(Long productId, List<MarketDto.ProductImageDto> images) {
        marketMapper.deleteProductImageAll(productId);
        for (MarketDto.ProductImageDto image : images) {
            image.setProductId(productId);
            marketMapper.insertProductImage(image);
        }
    }

    @Transactional
    public List<MarketDto.ProductImageDto> replaceProductImagesWithFiles(Long productId, List<MultipartFile> images) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            throw notFound("Product not found.");
        }
        marketMapper.deleteProductImageAll(productId);
        return uploadProductImages(productId, images);
    }

    @Transactional
    public List<MarketDto.ProductImageDto> updateProductImages(Long productId, List<Long> keepImageIds, List<MultipartFile> images) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            throw notFound("Product not found.");
        }
        marketMapper.deleteProductImagesExcept(productId, keepImageIds == null ? List.of() : keepImageIds);
        return uploadProductImages(productId, images);
    }

    public List<MarketDto.ProductImageDto> getProductImageList(Long productId, Long memberId) {
        assertProductAccessible(productId, memberId);
        return marketMapper.selectProductImageList(productId);
    }

    // ── 댓글 ──────────────────────────────────────────────────

    // 댓글 목록 조회 (대댓글 포함해서 조립)
    public List<MarketDto.ProductCommentDto> getProductCommentList(Long productId, Long memberId) {
        assertProductAccessible(productId, memberId);
        List<MarketDto.ProductCommentDto> comments = marketMapper.selectProductCommentList(productId);
        for (MarketDto.ProductCommentDto comment : comments) {
            comment.setReplies(marketMapper.selectProductReplyList(comment.getCommentId()));
        }
        return comments;
    }

    public MarketDto.ProductCommentDto findProductCommentById(Long commentId) {
        return marketMapper.selectProductCommentById(commentId);
    }

    // 댓글 등록
    @Transactional
    public int createProductComment(MarketDto.ProductCommentCreateDto createDto) {
        MarketDto.ProductDto product = requireAccessibleProduct(createDto.getProductId(), createDto.getMemberId());
        createDto.setUnivId(product.getUnivId());
        return marketMapper.insertProductComment(createDto);
    }

    // 댓글 삭제
    @Transactional
    public int deleteProductComment(Long commentId) {
        return marketMapper.deleteProductComment(commentId);
    }

    // ── 찜(좋아요) ────────────────────────────────────────────

    // 찜 토글 (찜 되어 있으면 취소, 없으면 추가)
    @Transactional
    public boolean toggleProductLike(MarketDto.ProductLikeDto likeDto) {
        assertProductAccessible(likeDto.getProductId(), likeDto.getMemberId());
        int exists = marketMapper.selectProductLikeCount(likeDto);
        if (exists > 0) {
            marketMapper.deleteProductLike(likeDto);
            return false; // 찜 취소
        } else {
            marketMapper.insertProductLike(likeDto);
            return true;  // 찜 추가
        }
    }

    public boolean isProductLiked(MarketDto.ProductLikeDto likeDto) {
        assertProductAccessible(likeDto.getProductId(), likeDto.getMemberId());
        return marketMapper.selectProductLikeCount(likeDto) > 0;
    }

    public int getProductLikeCount(Long productId, Long memberId) {
        assertProductAccessible(productId, memberId);
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            throw notFound("Product not found.");
        }
        return product.getLikeCount() == null ? 0 : product.getLikeCount();
    }

    @Transactional
    public Map<String, Object> reportProduct(MarketDto.ProductReportDto reportDto) {
        Map<String, Object> result = new HashMap<>();
        MarketDto.ProductDto product = marketMapper.selectProductDetail(reportDto.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            throw notFound("Product not found.");
        }
        if (product.getMemberId().equals(reportDto.getMemberId())) {
            throw forbidden("Seller cannot report own product.");
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(reportDto.getMemberId()));
        int exists = marketMapper.selectProductReportCount(reportDto);
        if (exists > 0) {
            result.put("success", false);
            result.put("message", "이미 신고한 상품입니다.");
            return result;
        }

        marketMapper.insertProductReport(reportDto);
        marketMapper.updateProductReportStatus(reportDto.getProductId());
        int reportCount = marketMapper.selectProductTotalReportCount(reportDto.getProductId());
        if (reportCount >= 5) {
            closeActiveTradeChatRoomsByProduct(reportDto.getProductId());
        }
        result.put("success", true);
        result.put("reportCount", reportCount);
        result.put("blind", reportCount >= 5);
        result.put("message", "신고가 접수되었습니다.");
        return result;
    }

    public boolean isProductReported(Long productId, Long memberId) {
        assertProductAccessible(productId, memberId);
        MarketDto.ProductReportDto reportDto = new MarketDto.ProductReportDto();
        reportDto.setProductId(productId);
        reportDto.setMemberId(memberId);
        return marketMapper.selectProductReportCount(reportDto) > 0;
    }

    public List<MarketDto.TradeChatRoomDto> getTradeChatRooms(Long memberId) {
        validateMember(memberId);
        return marketMapper.selectTradeChatRoomsForMember(memberId);
    }

    @Transactional
    public MarketDto.TradeChatRoomDto createOrGetTradeChatRoom(
            Long memberId,
            MarketDto.TradeChatRoomRequestDto request) {
        validateMember(memberId);
        if (request == null || request.getProductId() == null) {
            throw badRequest("Product id is required.");
        }

        MarketDto.ProductDto product = marketMapper.selectProductDetail(request.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            throw notFound("Product not found.");
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(memberId));
        if (product.getMemberId().equals(memberId)) {
            throw forbidden("Seller cannot create a buyer chat for own product.");
        }
        if ("DONE".equals(product.getProductStatus())) {
            throw conflict("Completed product cannot start a chat.");
        }
        if (isBlindProduct(product)) {
            throw conflict("Blind product cannot start a chat.");
        }

        MarketDto.TradeChatRoomDto existingRoom =
                marketMapper.selectTradeChatRoomByBuyer(product.getProductId(), memberId);
        if (existingRoom != null) {
            return existingRoom;
        }

        MarketDto.TradeChatRoomDto roomDto = MarketDto.TradeChatRoomDto.builder()
                .productId(product.getProductId())
                .sellerId(product.getMemberId())
                .buyerId(memberId)
                .negotiatedPrice(product.getPrice())
                .status(DEFAULT_TRADE_CHAT_STATUS)
                .build();
        marketMapper.insertTradeChatRoom(roomDto);

        if ("SALE".equals(product.getProductStatus())) {
            marketMapper.updateProductStatus(product.getProductId(), "RESERVE");
        }

        return marketMapper.selectTradeChatRoomForMember(roomDto.getRoomId(), memberId);
    }

    public List<MarketDto.TradeChatMessageDto> getTradeChatMessages(Long memberId, Long roomId) {
        requireTradeChatRoom(roomId, memberId);
        return marketMapper.selectTradeChatMessages(roomId);
    }

    @Transactional
    public MarketDto.TradeChatMessageDto sendTradeChatMessage(
            Long memberId,
            Long roomId,
            MarketDto.TradeChatMessageRequestDto request) {
        validateChatMessageRequest(request);
        MarketDto.TradeChatRoomDto room = requireTradeChatRoom(roomId, memberId);
        if (DONE_TRADE_CHAT_STATUS.equals(room.getStatus())
                || CLOSED_TRADE_CHAT_STATUS.equals(room.getStatus())
                || "DONE".equals(room.getProductStatus())) {
            throw conflict("Closed trade chat cannot send messages.");
        }

        MarketDto.TradeChatMessageDto messageDto = MarketDto.TradeChatMessageDto.builder()
                .roomId(roomId)
                .senderId(memberId)
                .content(request.getContent().trim())
                .isRead(0)
                .build();
        marketMapper.insertTradeChatMessage(messageDto);
        MarketDto.TradeChatMessageDto savedMessage =
                marketMapper.selectTradeChatMessage(messageDto.getMessageId());
        MarketDto.TradeChatMessageDto response =
                savedMessage == null ? messageDto : savedMessage;

        runAfterCommit(() -> messagingTemplate.convertAndSend(
                MARKET_CHAT_TOPIC_PREFIX + roomId,
                response));

        return response;
    }

    @Transactional
    public MarketDto.TradeChatRoomDto updateTradeChatNegotiatedPrice(
            Long memberId,
            Long roomId,
            MarketDto.TradeChatPriceRequestDto request) {
        MarketDto.TradeChatRoomDto room = requireTradeChatRoom(roomId, memberId);
        if (!room.getSellerId().equals(memberId)) {
            throw forbidden("Only seller can update negotiated price.");
        }
        if (request == null || request.getNegotiatedPrice() == null || request.getNegotiatedPrice() < 0) {
            throw badRequest("Negotiated price is required.");
        }
        if ("DONE".equals(room.getProductStatus())
                || DONE_TRADE_CHAT_STATUS.equals(room.getStatus())
                || CLOSED_TRADE_CHAT_STATUS.equals(room.getStatus())) {
            throw conflict("Completed trade chat cannot update price.");
        }

        marketMapper.updateTradeChatNegotiatedPrice(roomId, request.getNegotiatedPrice());
        MarketDto.TradeChatRoomDto updatedRoom =
                marketMapper.selectTradeChatRoomForMember(roomId, memberId);
        runAfterCommit(() -> messagingTemplate.convertAndSend(
                MARKET_CHAT_TOPIC_PREFIX + roomId + "/room",
                updatedRoom));
        return updatedRoom;
    }

    @Transactional
    public MarketDto.TradeChatRoomDto completeFreeTradeChat(Long memberId, Long roomId) {
        MarketDto.TradeChatRoomDto room = requireTradeChatRoom(roomId, memberId);
        if (!room.getSellerId().equals(memberId)) {
            throw forbidden("Only seller can complete free sharing.");
        }
        if (DONE_TRADE_CHAT_STATUS.equals(room.getStatus()) || "DONE".equals(room.getProductStatus())) {
            throw conflict("Trade is already completed.");
        }
        if (CLOSED_TRADE_CHAT_STATUS.equals(room.getStatus())) {
            throw conflict("Closed trade chat cannot be completed.");
        }

        MarketDto.ProductDto product = marketMapper.selectProductDetail(room.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            throw notFound("Product not found.");
        }
        if (!product.getMemberId().equals(room.getSellerId())) {
            throw conflict("Product seller does not match chat seller.");
        }
        if (product.getPrice() == null || product.getPrice() > 0) {
            throw conflict("Only free sharing products can be completed without payment.");
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(memberId));

        MarketDto.TradeDto tradeDto = MarketDto.TradeDto.builder()
                .productId(product.getProductId())
                .sellerId(room.getSellerId())
                .buyerId(room.getBuyerId())
                .tradeStatus("DONE")
                .price(0L)
                .build();
        marketMapper.insertTrade(tradeDto);
        marketMapper.updateProductStatus(product.getProductId(), "DONE");
        marketMapper.updateTradeChatStatus(room.getRoomId(), DONE_TRADE_CHAT_STATUS);

        MarketDto.TradeChatRoomDto updatedRoom =
                marketMapper.selectTradeChatRoomForMember(roomId, memberId);
        runAfterCommit(() -> messagingTemplate.convertAndSend(
                MARKET_CHAT_TOPIC_PREFIX + roomId + "/room",
                updatedRoom));
        return updatedRoom;
    }
    @Transactional
    public MarketDto.TradeChatRoomDto closeTradeChatRoom(Long memberId, Long roomId) {
        MarketDto.TradeChatRoomDto room = requireTradeChatRoom(roomId, memberId);
        if (DONE_TRADE_CHAT_STATUS.equals(room.getStatus())) {
            throw conflict("Completed trade chat cannot be closed.");
        }
        if (!CLOSED_TRADE_CHAT_STATUS.equals(room.getStatus())) {
            marketMapper.updateTradeChatStatus(roomId, CLOSED_TRADE_CHAT_STATUS);
        }
        restoreReservableProductIfNoActiveChats(room.getProductId());

        MarketDto.TradeChatRoomDto updatedRoom =
                marketMapper.selectTradeChatRoomForMember(roomId, memberId);
        runAfterCommit(() -> messagingTemplate.convertAndSend(
                MARKET_CHAT_TOPIC_PREFIX + roomId + "/room",
                updatedRoom));
        return updatedRoom;
    }

    @Transactional
    public Map<String, Object> deleteTradeChatRoom(Long memberId, Long roomId) {
        MarketDto.TradeChatRoomDto room = requireTradeChatRoom(roomId, memberId);
        boolean deletableStatus = CLOSED_TRADE_CHAT_STATUS.equals(room.getStatus())
                || DONE_TRADE_CHAT_STATUS.equals(room.getStatus())
                || "DONE".equals(room.getProductStatus());
        if (!deletableStatus) {
            throw conflict("Only closed or completed trade chat can be deleted.");
        }

        marketMapper.deleteTradeChatMessages(roomId);
        int rows = marketMapper.deleteTradeChatRoom(roomId);
        if (CLOSED_TRADE_CHAT_STATUS.equals(room.getStatus())) {
            restoreReservableProductIfNoActiveChats(room.getProductId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", rows > 0);
        result.put("productId", room.getProductId());
        result.put("message", rows > 0 ? "채팅방이 삭제되었습니다." : "삭제할 채팅방이 없습니다.");
        return result;
    }

    // 내 찜 목록
    public List<MarketDto.ProductDto> getMyLikeList(Long memberId) {
        CommunityAccessScope scope = communityAccessService.getScope(memberId);
        List<MarketDto.ProductDto> products = marketMapper.selectMyLikeList(memberId, scope.getQueryUnivId());
        for (MarketDto.ProductDto product : products) {
            product.setImages(marketMapper.selectProductImageList(product.getProductId()));
        }
        return products;
    }

    public MarketDto.PaymentConfigDto getPaymentConfig() {
        MarketDto.PaymentConfigDto configDto = new MarketDto.PaymentConfigDto();
        configDto.setStoreId(portoneTradeStoreId);
        configDto.setKgInicisChannelKey(portoneKgInicisChannelKey);
        configDto.setKakaoPayChannelKey(portoneKakaoPayChannelKey);
        return configDto;
    }

    @Transactional
    public MarketDto.PaymentResultDto completePayment(MarketDto.PaymentCompleteDto completeDto, Long buyerId) {
        validatePaymentCompleteRequest(completeDto);
        MarketDto.ProductDto product = marketMapper.selectProductDetail(completeDto.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            throw notFound("Product not found.");
        }
        if (!"SALE".equals(product.getProductStatus())) {
            throw conflict("Product is not available for payment.");
        }
        if (product.getMemberId().equals(buyerId)) {
            throw forbidden("Seller cannot buy own product.");
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(buyerId));

        verifyPortOnePayment(completeDto, product);

        MarketDto.TradeDto tradeDto = MarketDto.TradeDto.builder()
                .productId(product.getProductId())
                .sellerId(product.getMemberId())
                .buyerId(buyerId)
                .tradeStatus("DONE")
                .price(product.getPrice())
                .build();
        marketMapper.insertTrade(tradeDto);

        MarketDto.PaymentDto paymentDto = MarketDto.PaymentDto.builder()
                .tradeId(tradeDto.getTradeId())
                .impUid(completeDto.getPaymentId())
                .merchantUid(completeDto.getPaymentId())
                .amount(product.getPrice())
                .status("PAID")
                .build();
        marketMapper.insertPayment(paymentDto);
        marketMapper.updateProductStatus(product.getProductId(), "DONE");

        return MarketDto.PaymentResultDto.builder()
                .tradeId(tradeDto.getTradeId())
                .paymentId(paymentDto.getPaymentId())
                .productStatus("DONE")
                .paymentStatus("PAID")
                .build();
    }

    @Transactional
    public MarketDto.PaymentResultDto completeChatPayment(
            MarketDto.ChatPaymentCompleteDto completeDto,
            Long buyerId) {
        validateChatPaymentCompleteRequest(completeDto);
        MarketDto.TradeChatRoomDto room =
                requireTradeChatRoom(completeDto.getRoomId(), buyerId);
        if (!room.getBuyerId().equals(buyerId)) {
            throw forbidden("Only buyer can pay in trade chat.");
        }
        if (DONE_TRADE_CHAT_STATUS.equals(room.getStatus()) || "DONE".equals(room.getProductStatus())) {
            throw conflict("Trade is already completed.");
        }
        if (CLOSED_TRADE_CHAT_STATUS.equals(room.getStatus())) {
            throw conflict("Closed trade chat cannot be paid.");
        }

        MarketDto.ProductDto product = marketMapper.selectProductDetail(room.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            throw notFound("Product not found.");
        }
        if (!product.getMemberId().equals(room.getSellerId())) {
            throw conflict("Product seller does not match chat seller.");
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(buyerId));

        Long paymentAmount = room.getNegotiatedPrice() == null
                ? product.getPrice()
                : room.getNegotiatedPrice();
        verifyPortOnePaymentAmount(completeDto.getPaymentId(), paymentAmount);

        MarketDto.TradeDto tradeDto = MarketDto.TradeDto.builder()
                .productId(product.getProductId())
                .sellerId(room.getSellerId())
                .buyerId(room.getBuyerId())
                .tradeStatus("DONE")
                .price(paymentAmount)
                .build();
        marketMapper.insertTrade(tradeDto);

        MarketDto.PaymentDto paymentDto = MarketDto.PaymentDto.builder()
                .tradeId(tradeDto.getTradeId())
                .impUid(completeDto.getPaymentId())
                .merchantUid(completeDto.getPaymentId())
                .amount(paymentAmount)
                .status("PAID")
                .build();
        marketMapper.insertPayment(paymentDto);
        marketMapper.updateProductStatus(product.getProductId(), "DONE");
        marketMapper.updateTradeChatStatus(room.getRoomId(), DONE_TRADE_CHAT_STATUS);

        return MarketDto.PaymentResultDto.builder()
                .tradeId(tradeDto.getTradeId())
                .paymentId(paymentDto.getPaymentId())
                .productStatus("DONE")
                .paymentStatus("PAID")
                .build();
    }

    private void verifyPortOnePayment(MarketDto.PaymentCompleteDto completeDto, MarketDto.ProductDto product) {
        verifyPortOnePaymentAmount(completeDto.getPaymentId(), product.getPrice());
    }

    private void verifyPortOnePaymentAmount(String requestPaymentId, Long expectedAmount) {
        if (isBlank(portoneTradeApiSecret)) {
            throw badGateway("PortOne API credentials are missing.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneTradeApiSecret);

        ResponseEntity<PortOneTradePaymentResponse> response;
        try {
            response = restTemplate.exchange(
                    "https://api.portone.io/payments/" + requestPaymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PortOneTradePaymentResponse.class
            );
        } catch (RestClientResponseException ex) {
            throw badGateway(
                    "PortOne payment lookup failed. status=" + ex.getStatusCode().value()
            );
        } catch (RestClientException ex) {
            throw badGateway("PortOne payment lookup failed.");
        }

        PortOneTradePaymentResponse payment = response.getBody();
        if (payment == null) {
            throw badGateway("Payment information not found.");
        }

        String paymentId = payment.id;
        String status = payment.status;
        Long amount = payment.amount == null ? null : payment.amount.total;
        String channelKey = payment.channel == null ? null : payment.channel.key;

        if (!requestPaymentId.equals(paymentId)) {
            throw conflict("Payment id does not match.");
        }
        if (!"PAID".equals(status)) {
            throw conflict("Payment is not paid.");
        }
        if (amount == null || !amount.equals(expectedAmount)) {
            throw conflict("Payment amount does not match.");
        }
        if (!isAllowedTradeChannelKey(channelKey)) {
            throw conflict("Payment channel does not match.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAllowedTradeChannelKey(String channelKey) {
        return !isBlank(channelKey)
                && (channelKey.equals(portoneKgInicisChannelKey)
                || channelKey.equals(portoneKakaoPayChannelKey));
    }

    private void validatePaymentCompleteRequest(MarketDto.PaymentCompleteDto completeDto) {
        if (completeDto == null) {
            throw badRequest("Payment request body is required.");
        }
        if (completeDto.getProductId() == null) {
            throw badRequest("Product id is required.");
        }
        if (isBlank(completeDto.getPaymentId())) {
            throw badRequest("paymentId is required.");
        }
    }

    private void validateChatPaymentCompleteRequest(MarketDto.ChatPaymentCompleteDto completeDto) {
        if (completeDto == null) {
            throw badRequest("Payment request body is required.");
        }
        if (completeDto.getRoomId() == null) {
            throw badRequest("Chat room id is required.");
        }
        if (isBlank(completeDto.getPaymentId())) {
            throw badRequest("paymentId is required.");
        }
    }

    private void validateMember(Long memberId) {
        if (memberId == null) {
            throw unauthorized("Login is required.");
        }
    }

    public void assertProductAccessible(Long productId, Long memberId) {
        requireAccessibleProduct(productId, memberId);
    }

    private MarketDto.ProductDto requireAccessibleProduct(Long productId, Long memberId) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            throw notFound("Product not found.");
        }
        communityAccessService.assertAccessible(product.getUnivId(), communityAccessService.getScope(memberId));
        return product;
    }

    private void applySchoolScope(MarketDto.ProductSearchDto searchDto, Long memberId) {
        CommunityAccessScope scope = communityAccessService.getScope(memberId);
        if (searchDto.getPage() == null || searchDto.getPage() < 0) {
            searchDto.setPage(0);
        }
        if (searchDto.getSize() == null || searchDto.getSize() <= 0) {
            searchDto.setSize(10);
        }
        searchDto.setViewerMemberId(memberId);
        searchDto.setUnivId(scope.getQueryUnivId());
    }

    private MarketDto.TradeChatRoomDto requireTradeChatRoom(Long roomId, Long memberId) {
        validateMember(memberId);
        if (roomId == null) {
            throw badRequest("Chat room id is required.");
        }

        MarketDto.TradeChatRoomDto room =
                marketMapper.selectTradeChatRoomForMember(roomId, memberId);
        if (room == null) {
            throw notFound("Trade chat room not found.");
        }
        return room;
    }

    private void validateChatMessageRequest(MarketDto.TradeChatMessageRequestDto request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw badRequest("Message content is required.");
        }
        if (request.getContent().trim().length() > MAX_CHAT_MESSAGE_LENGTH) {
            throw badRequest("Message content must be 2000 characters or less.");
        }
    }

    private void validateEditableProductStatus(String productStatus) {
        if (!"SALE".equals(productStatus) && !"RESERVE".equals(productStatus)) {
            throw badRequest("Product status can only be changed to SALE or RESERVE.");
        }
    }

    private void restoreReservableProductIfNoActiveChats(Long productId) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null || product.getIsDeleted() == 1) {
            return;
        }
        if (!"RESERVE".equals(product.getProductStatus())) {
            return;
        }
        if (marketMapper.selectActiveTradeChatRoomCountByProduct(productId) == 0) {
            marketMapper.updateProductStatus(productId, "SALE");
        }
    }

    private int closeActiveTradeChatRoomsByProduct(Long productId) {
        return marketMapper.updateActiveTradeChatStatusByProduct(productId, CLOSED_TRADE_CHAT_STATUS);
    }

    private boolean isBlindProduct(MarketDto.ProductDto product) {
        return Integer.valueOf(1).equals(product.getIsBlind())
                || (product.getReportCount() != null && product.getReportCount() >= 5);
    }

    private void validateProductImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw badRequest("Only JPG, PNG, and WEBP images can be uploaded.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw badRequest("Image size must be 30MB or less.");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException badGateway(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    public static class PortOneTradePaymentResponse {
        public String id;
        public String status;
        public PortOneTradePaymentAmount amount;
        public PortOneTradePaymentChannel channel;
    }

    public static class PortOneTradePaymentAmount {
        public Long total;
    }

    public static class PortOneTradePaymentChannel {
        public String key;
    }
}
