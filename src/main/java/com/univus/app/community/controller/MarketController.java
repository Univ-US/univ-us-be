package com.univus.app.community.controller;

import com.univus.app.community.dto.MarketDto;
import com.univus.app.community.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @PathVariable Long productId) {

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
            @RequestBody MarketDto.ProductCreateDto createDto) {

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
            @PathVariable Long productId,
            @RequestBody MarketDto.ProductUpdateDto updateDto) {

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
            @PathVariable Long productId) {

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
            @PathVariable Long productId) {

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
            @PathVariable Long productId,
            @RequestBody MarketDto.ProductCommentCreateDto createDto) {

        createDto.setProductId(productId);
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
            @PathVariable Long commentId) {

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
            @PathVariable Long productId,
            @RequestBody MarketDto.ProductLikeDto likeDto) {

        likeDto.setProductId(productId);
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
            @RequestParam Long memberId) {

        List<MarketDto.ProductDto> list = marketService.getMyLikeList(memberId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("list", list);
        return ResponseEntity.ok(result);
    }
}