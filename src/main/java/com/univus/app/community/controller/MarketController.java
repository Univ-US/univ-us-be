package com.univus.app.community.controller;

import com.univus.app.community.dto.MarketDto;
import com.univus.app.community.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
            @ModelAttribute MarketDto.ProductSearchDto searchDto) {

        List<MarketDto.ProductDto> list  = marketService.getProductList(searchDto);
        int totalCount                   = marketService.getProductCount(searchDto);

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
            @PathVariable("productId") Long productId) {

        MarketDto.ProductDto product = marketService.getProductDetail(productId);

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
    // GET /api/market/products/{productId}/comments
    @GetMapping("/products/{productId}/comments")
    public ResponseEntity<Map<String, Object>> getCommentList(
            @PathVariable("productId") Long productId) {

        List<MarketDto.ProductCommentDto> comments =
                marketService.getProductCommentList(productId);

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
            @PathVariable("commentId") Long commentId) {

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

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("liked", liked);
        result.put("message", liked ? "찜했습니다." : "찜을 취소했습니다.");
        return ResponseEntity.ok(result);
    }

    // 내 찜 목록
    // GET /api/market/likes?memberId=1
    @GetMapping("/likes")
    public ResponseEntity<Map<String, Object>> getMyLikeList(
            @RequestParam("memberId") Long memberId) {

        List<MarketDto.ProductDto> list = marketService.getMyLikeList(memberId);

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
            @RequestBody MarketDto.PaymentCompleteDto completeDto) {
        MarketDto.PaymentResultDto paymentResult = marketService.completePayment(completeDto);

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
        if (!product.getMemberId().equals(memberId) && !isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상품 수정/삭제 권한이 없습니다.");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_SUA".equals(authority.getAuthority())
                                || "ROLE_ADM".equals(authority.getAuthority()));
    }
}
