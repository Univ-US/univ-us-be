package com.univus.app.community.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.univus.app.community.dto.MarketDto;
import com.univus.app.community.mapper.MarketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketMapper marketMapper;
    private final RestTemplate restTemplate;

    @Value("${portone.imp.code:}")
    private String portoneImpCode;

    @Value("${portone.api.key:}")
    private String portoneApiKey;

    @Value("${portone.api.secret:}")
    private String portoneApiSecret;

    // ── 상품 ──────────────────────────────────────────────────

    // 상품 목록 조회
    public List<MarketDto.ProductDto> getProductList(MarketDto.ProductSearchDto searchDto) {
        return marketMapper.selectProductList(searchDto);
    }

    // 상품 전체 개수 (페이징용)
    public int getProductCount(MarketDto.ProductSearchDto searchDto) {
        return marketMapper.selectProductCount(searchDto);
    }

    // 상품 상세 조회 + 조회수 증가
    @Transactional
    public MarketDto.ProductDto getProductDetail(Long productId) {
        marketMapper.updateViewCount(productId);
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product != null) {
            product.setImages(marketMapper.selectProductImageList(productId));
        }
        return product;
    }

    // 상품 등록
    @Transactional
    public int createProduct(MarketDto.ProductCreateDto createDto) {
        return marketMapper.insertProduct(createDto);
    }

    // 상품 수정
    @Transactional
    public int updateProduct(MarketDto.ProductUpdateDto updateDto) {
        return marketMapper.updateProduct(updateDto);
    }

    // 상품 삭제
    @Transactional
    public int deleteProduct(Long productId) {
        return marketMapper.deleteProduct(productId);
    }

    // ── 이미지 ────────────────────────────────────────────────

    // 이미지 등록
    @Transactional
    public int addProductImage(MarketDto.ProductImageDto imageDto) {
        return marketMapper.insertProductImage(imageDto);
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

    // ── 댓글 ──────────────────────────────────────────────────

    // 댓글 목록 조회 (대댓글 포함해서 조립)
    public List<MarketDto.ProductCommentDto> getProductCommentList(Long productId) {
        List<MarketDto.ProductCommentDto> comments = marketMapper.selectProductCommentList(productId);
        for (MarketDto.ProductCommentDto comment : comments) {
            comment.setReplies(marketMapper.selectProductReplyList(comment.getCommentId()));
        }
        return comments;
    }

    // 댓글 등록
    @Transactional
    public int createProductComment(MarketDto.ProductCommentCreateDto createDto) {
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
        int exists = marketMapper.selectProductLikeCount(likeDto);
        if (exists > 0) {
            marketMapper.deleteProductLike(likeDto);
            return false; // 찜 취소
        } else {
            marketMapper.insertProductLike(likeDto);
            return true;  // 찜 추가
        }
    }

    // 내 찜 목록
    public List<MarketDto.ProductDto> getMyLikeList(Long memberId) {
        return marketMapper.selectMyLikeList(memberId);
    }

    public MarketDto.PaymentConfigDto getPaymentConfig() {
        MarketDto.PaymentConfigDto configDto = new MarketDto.PaymentConfigDto();
        configDto.setImpCode(portoneImpCode);
        return configDto;
    }

    @Transactional
    public MarketDto.PaymentResultDto completePayment(MarketDto.PaymentCompleteDto completeDto) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(completeDto.getProductId());
        if (product == null || product.getIsDeleted() == 1) {
            throw new IllegalArgumentException("Product not found.");
        }
        if (!"SALE".equals(product.getProductStatus())) {
            throw new IllegalStateException("Product is not available for payment.");
        }

        Long buyerId = completeDto.getBuyerId() != null ? completeDto.getBuyerId() : 1L;
        if (product.getMemberId().equals(buyerId)) {
            throw new IllegalStateException("Seller cannot buy own product.");
        }

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
                .impUid(completeDto.getImpUid())
                .merchantUid(completeDto.getMerchantUid())
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

    private void verifyPortOnePayment(MarketDto.PaymentCompleteDto completeDto, MarketDto.ProductDto product) {
        if (isBlank(portoneApiKey) || isBlank(portoneApiSecret)) {
            throw new IllegalStateException("PortOne API credentials are missing.");
        }

        String accessToken = requestPortOneAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "https://api.iamport.kr/payments/" + completeDto.getImpUid(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
        );

        JsonNode payment = response.getBody() == null ? null : response.getBody().path("response");
        if (payment == null || payment.isMissingNode() || payment.isNull()) {
            throw new IllegalStateException("Payment information not found.");
        }

        String merchantUid = payment.path("merchant_uid").asText();
        String status = payment.path("status").asText();
        long amount = payment.path("amount").asLong();

        if (!completeDto.getMerchantUid().equals(merchantUid)) {
            throw new IllegalStateException("Merchant uid does not match.");
        }
        if (!"paid".equals(status)) {
            throw new IllegalStateException("Payment is not paid.");
        }
        if (amount != product.getPrice()) {
            throw new IllegalStateException("Payment amount does not match.");
        }
    }

    private String requestPortOneAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("imp_key", portoneApiKey);
        body.put("imp_secret", portoneApiSecret);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "https://api.iamport.kr/users/getToken",
                new HttpEntity<>(body, headers),
                JsonNode.class
        );

        String accessToken = response.getBody()
                .path("response")
                .path("access_token")
                .asText();
        if (isBlank(accessToken)) {
            throw new IllegalStateException("Failed to issue PortOne access token.");
        }
        return accessToken;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
