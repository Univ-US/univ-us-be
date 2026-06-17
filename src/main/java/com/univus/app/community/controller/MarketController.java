package com.univus.app.community.controller;

import com.univus.app.community.dto.MarketDto;
import com.univus.app.community.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    // ── 상품 ──────────────────────────────────────────────────

    // 상품 목록 조회
    // GET /api/market/products?keyword=&category=&productStatus=&page=0&size=10
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProductList(
            @ModelAttribute MarketDto.ProductSearchDto searchDto,
            @AuthenticationPrincipal Long memberId) {

        Long currentMemberId = requireMemberId(memberId);
        List<MarketDto.ProductDto> list  = marketService.getProductList(searchDto, currentMemberId);
        int totalCount                   = marketService.getProductCount(searchDto, currentMemberId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("list", list);
        result.put("totalCount", totalCount);
        return ResponseEntity.ok(result);
    }

    // 상품 상세 조회
    // GET /api/market/products/{productId}
    @GetMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> getProductDetail(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId) {

        MarketDto.ProductDto product = marketService.getProductDetail(productId, requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        if (product == null) {
            result.put("success", false);
            result.put("message", "상품을 찾을 수 없습니다.");
            return ResponseEntity.status(404).body(result);
        }
        result.put("success", true);
        result.put("product", product);
        return ResponseEntity.ok(result);
    }

    // 상품 등록
    // POST /api/market/products
    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MarketDto.ProductCreateDto createDto) {

        createDto.setMemberId(requireMemberId(memberId));
        int rows = marketService.createProduct(createDto);

        Map<String, Object> result = new HashMap<>();
        result.put("success", rows > 0);
        result.put("productId", createDto.getProductId());
        result.put("message", rows > 0 ? "상품이 등록되었습니다." : "등록에 실패했습니다.");
        return ResponseEntity.ok(result);
    }

    // 상품 수정
    // PUT /api/market/products/{productId}
    @PutMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication,
            @RequestBody MarketDto.ProductUpdateDto updateDto) {

        assertCanManageProduct(productId, requireMemberId(memberId), authentication);
        updateDto.setProductId(productId);
        int rows = marketService.updateProduct(updateDto);

        Map<String, Object> result = new HashMap<>();
        result.put("success", rows > 0);
        result.put("message", rows > 0 ? "상품이 수정되었습니다." : "수정에 실패했습니다.");
        return ResponseEntity.ok(result);
    }

    // 무료 나눔 완료
    // PATCH /api/market/products/{productId}/free-complete
    @PatchMapping("/products/{productId}/free-complete")
    public ResponseEntity<Map<String, Object>> completeFreeProduct(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication) {

        assertCanManageProduct(productId, requireMemberId(memberId), authentication);
        MarketDto.ProductDto product = marketService.completeFreeProduct(productId, requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("product", product);
        return ResponseEntity.ok(result);
    }
    // 상품 삭제
    // DELETE /api/market/products/{productId}
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication) {

        assertCanManageProduct(productId, requireMemberId(memberId), authentication);
        int rows = marketService.deleteProduct(productId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", rows > 0);
        result.put("message", rows > 0 ? "상품이 삭제되었습니다." : "삭제에 실패했습니다.");
        return ResponseEntity.ok(result);
    }

    // ── 댓글 ──────────────────────────────────────────────────

    // 댓글 목록 조회
    // 이미지
    // POST /api/market/products/{productId}/images
    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadProductImages(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication,
            @RequestParam("images") List<MultipartFile> images) {

        assertCanManageProduct(productId, requireMemberId(memberId), authentication);
        List<MarketDto.ProductImageDto> uploadedImages = marketService.uploadProductImages(productId, images);
        return ResponseEntity.ok(Map.of(
                "message", "Images uploaded successfully.",
                "images", uploadedImages
        ));
    }

    // PUT /api/market/products/{productId}/images
    @PutMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> replaceProductImages(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "keepImageIds", required = false) List<Long> keepImageIds) {

        assertCanManageProduct(productId, requireMemberId(memberId), authentication);
        List<MarketDto.ProductImageDto> uploadedImages =
                marketService.updateProductImages(productId, keepImageIds, images);
        return ResponseEntity.ok(Map.of(
                "message", "Images updated successfully.",
                "images", uploadedImages
        ));
    }

    // GET /api/market/products/{productId}/images
    @GetMapping("/products/{productId}/images")
    public ResponseEntity<List<MarketDto.ProductImageDto>> getProductImages(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(marketService.getProductImageList(productId, requireMemberId(memberId)));
    }

    // 댓글
    // GET /api/market/products/{productId}/comments
    @GetMapping("/products/{productId}/comments")
    public ResponseEntity<Map<String, Object>> getCommentList(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId) {

        List<MarketDto.ProductCommentDto> comments =
                marketService.getProductCommentList(productId, requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("comments", comments);
        return ResponseEntity.ok(result);
    }

    // 댓글 등록
    // POST /api/market/products/{productId}/comments
    @PostMapping("/products/{productId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            @RequestBody MarketDto.ProductCommentCreateDto createDto) {

        createDto.setProductId(productId);
        createDto.setMemberId(requireMemberId(memberId));
        int rows = marketService.createProductComment(createDto);

        Map<String, Object> result = new HashMap<>();
        result.put("success", rows > 0);
        result.put("message", rows > 0 ? "댓글이 등록되었습니다." : "등록에 실패했습니다.");
        return ResponseEntity.ok(result);
    }

    // 댓글 삭제
    // DELETE /api/market/comments/{commentId}
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal Long memberId,
            Authentication authentication) {

        assertCanManageProductComment(commentId, requireMemberId(memberId), authentication);
        int rows = marketService.deleteProductComment(commentId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", rows > 0);
        result.put("message", rows > 0 ? "댓글이 삭제되었습니다." : "삭제에 실패했습니다.");
        return ResponseEntity.ok(result);
    }

    // ── 찜(좋아요) ────────────────────────────────────────────

    // 찜 토글
    // POST /api/market/products/{productId}/like
    @PostMapping("/products/{productId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            @RequestBody MarketDto.ProductLikeDto likeDto) {

        likeDto.setProductId(productId);
        likeDto.setMemberId(requireMemberId(memberId));
        boolean liked = marketService.toggleProductLike(likeDto);
        int likeCount = marketService.getProductLikeCount(productId, requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        result.put("message", liked ? "찜했습니다." : "찜을 취소했습니다.");
        return ResponseEntity.ok(result);
    }

    // GET /api/market/products/{productId}/like
    @GetMapping("/products/{productId}/like")
    public ResponseEntity<Map<String, Object>> getLikeStatus(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId) {

        MarketDto.ProductLikeDto likeDto = new MarketDto.ProductLikeDto();
        likeDto.setProductId(productId);
        likeDto.setMemberId(requireMemberId(memberId));
        boolean liked = marketService.isProductLiked(likeDto);
        int likeCount = marketService.getProductLikeCount(productId, requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        return ResponseEntity.ok(result);
    }

    // POST /api/market/products/{productId}/report
    @PostMapping("/products/{productId}/report")
    public ResponseEntity<Map<String, Object>> reportProduct(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId,
            @RequestBody MarketDto.ProductReportDto reportDto) {

        reportDto.setProductId(productId);
        reportDto.setMemberId(requireMemberId(memberId));
        return ResponseEntity.ok(marketService.reportProduct(reportDto));
    }

    // GET /api/market/products/{productId}/report
    @GetMapping("/products/{productId}/report")
    public ResponseEntity<Map<String, Object>> getProductReportStatus(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Long memberId) {

        boolean reported = marketService.isProductReported(productId, requireMemberId(memberId));
        return ResponseEntity.ok(Map.of("reported", reported));
    }

    // 내 찜 목록
    // GET /api/market/likes?memberId=1
    @GetMapping("/likes")
    public ResponseEntity<Map<String, Object>> getMyLikeList(
            @AuthenticationPrincipal Long memberId) {

        List<MarketDto.ProductDto> list = marketService.getMyLikeList(requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("list", list);
        return ResponseEntity.ok(result);
    }

    // GET /api/market/payments/config
    @GetMapping("/payments/config")
    public ResponseEntity<MarketDto.PaymentConfigDto> getPaymentConfig() {
        return ResponseEntity.ok(marketService.getPaymentConfig());
    }

    // POST /api/market/payments/complete
    @PostMapping("/payments/complete")
    public ResponseEntity<Map<String, Object>> completePayment(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MarketDto.PaymentCompleteDto completeDto) {
        MarketDto.PaymentResultDto paymentResult =
                marketService.completePayment(completeDto, requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("payment", paymentResult);
        return ResponseEntity.ok(result);
    }

    // GET /api/market/chats
    @GetMapping("/chats")
    public ResponseEntity<List<MarketDto.TradeChatRoomDto>> getTradeChatRooms(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(marketService.getTradeChatRooms(requireMemberId(memberId)));
    }

    // POST /api/market/chats
    @PostMapping("/chats")
    public ResponseEntity<MarketDto.TradeChatRoomDto> createOrGetTradeChatRoom(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MarketDto.TradeChatRoomRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marketService.createOrGetTradeChatRoom(requireMemberId(memberId), request));
    }

    // GET /api/market/chats/{roomId}/messages
    @GetMapping("/chats/{roomId}/messages")
    public ResponseEntity<List<MarketDto.TradeChatMessageDto>> getTradeChatMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(
                marketService.getTradeChatMessages(requireMemberId(memberId), roomId));
    }

    // PATCH /api/market/chats/{roomId}/read
    @PatchMapping("/chats/{roomId}/read")
    public ResponseEntity<Map<String, Object>> markTradeChatMessagesAsRead(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        int readCount = marketService.markTradeChatMessagesAsRead(requireMemberId(memberId), roomId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "readCount", readCount
        ));
    }

    // POST /api/market/chats/{roomId}/messages
    @PostMapping("/chats/{roomId}/messages")
    public ResponseEntity<MarketDto.TradeChatMessageDto> sendTradeChatMessage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId,
            @RequestBody MarketDto.TradeChatMessageRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(marketService.sendTradeChatMessage(requireMemberId(memberId), roomId, request));
    }

    // PATCH /api/market/chats/{roomId}/price
    @PatchMapping("/chats/{roomId}/price")
    public ResponseEntity<MarketDto.TradeChatRoomDto> updateTradeChatNegotiatedPrice(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId,
            @RequestBody MarketDto.TradeChatPriceRequestDto request) {
        return ResponseEntity.ok(marketService.updateTradeChatNegotiatedPrice(
                requireMemberId(memberId),
                roomId,
                request));
    }

    // PATCH /api/market/chats/{roomId}/free-complete
    @PatchMapping("/chats/{roomId}/free-complete")
    public ResponseEntity<MarketDto.TradeChatRoomDto> completeFreeTradeChat(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(marketService.completeFreeTradeChat(requireMemberId(memberId), roomId));
    }
    // PATCH /api/market/chats/{roomId}/close
    @PatchMapping("/chats/{roomId}/close")
    public ResponseEntity<MarketDto.TradeChatRoomDto> closeTradeChatRoom(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(marketService.closeTradeChatRoom(requireMemberId(memberId), roomId));
    }

    // DELETE /api/market/chats/{roomId}
    @DeleteMapping("/chats/{roomId}")
    public ResponseEntity<Map<String, Object>> deleteTradeChatRoom(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(marketService.deleteTradeChatRoom(requireMemberId(memberId), roomId));
    }

    // POST /api/market/chats/payments/complete
    @PostMapping("/chats/payments/complete")
    public ResponseEntity<Map<String, Object>> completeChatPayment(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MarketDto.ChatPaymentCompleteDto completeDto) {
        MarketDto.PaymentResultDto paymentResult =
                marketService.completeChatPayment(completeDto, requireMemberId(memberId));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("payment", paymentResult);
        return ResponseEntity.ok(result);
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return memberId;
    }

    private void assertCanManageProduct(Long productId, Long memberId, Authentication authentication) {
        MarketDto.ProductDto product = marketService.findProductById(productId);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        marketService.assertProductAccessible(productId, memberId);
        if (!product.getMemberId().equals(memberId) && !isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상품 수정/삭제 권한이 없습니다.");
        }
    }

    private void assertCanManageProductComment(Long commentId, Long memberId, Authentication authentication) {
        MarketDto.ProductCommentDto comment = marketService.findProductCommentById(commentId);
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        marketService.assertProductAccessible(comment.getProductId(), memberId);
        if (!comment.getMemberId().equals(memberId) && !isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다.");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_SUA".equals(authority.getAuthority())
                                || "ROLE_ADM".equals(authority.getAuthority()));
    }
}
