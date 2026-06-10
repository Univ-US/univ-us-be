package com.univus.app.community.service;

import com.univus.app.common.StorageService;
import com.univus.app.community.dto.MarketDto;
import com.univus.app.community.mapper.MarketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketMapper marketMapper;
    private final RestTemplate restTemplate;
    private final StorageService storageService;

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

    // ── 상품 ──────────────────────────────────────────────────

    // 상품 목록 조회
    public List<MarketDto.ProductDto> getProductList(MarketDto.ProductSearchDto searchDto) {
        List<MarketDto.ProductDto> products = marketMapper.selectProductList(searchDto);
        for (MarketDto.ProductDto product : products) {
            product.setImages(marketMapper.selectProductImageList(product.getProductId()));
        }
        return products;
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

    public MarketDto.ProductDto findProductById(Long productId) {
        return marketMapper.selectProductDetail(productId);
    }

    // 상품 등록
    @Transactional
    public int createProduct(MarketDto.ProductCreateDto createDto) {
        createDto.setProductStatus("SALE");
        return marketMapper.insertProduct(createDto);
    }

    // 상품 수정
    @Transactional
    public int updateProduct(MarketDto.ProductUpdateDto updateDto) {
        validateEditableProductStatus(updateDto.getProductStatus());
        MarketDto.ProductDto product = marketMapper.selectProductDetail(updateDto.getProductId());
        if (product != null && "DONE".equals(product.getProductStatus())) {
            throw new IllegalStateException("Completed product cannot be edited.");
        }
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

    @Transactional
    public List<MarketDto.ProductImageDto> uploadProductImages(Long productId, List<MultipartFile> images) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found.");
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
            throw new IllegalArgumentException("Product not found.");
        }
        marketMapper.deleteProductImageAll(productId);
        return uploadProductImages(productId, images);
    }

    public List<MarketDto.ProductImageDto> getProductImageList(Long productId) {
        return marketMapper.selectProductImageList(productId);
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

    public boolean isProductLiked(MarketDto.ProductLikeDto likeDto) {
        return marketMapper.selectProductLikeCount(likeDto) > 0;
    }

    public int getProductLikeCount(Long productId) {
        MarketDto.ProductDto product = marketMapper.selectProductDetail(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found.");
        }
        return product.getLikeCount() == null ? 0 : product.getLikeCount();
    }

    // 내 찜 목록
    public List<MarketDto.ProductDto> getMyLikeList(Long memberId) {
        List<MarketDto.ProductDto> products = marketMapper.selectMyLikeList(memberId);
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
            throw new IllegalArgumentException("Product not found.");
        }
        if (!"SALE".equals(product.getProductStatus())) {
            throw new IllegalStateException("Product is not available for payment.");
        }
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

    private void verifyPortOnePayment(MarketDto.PaymentCompleteDto completeDto, MarketDto.ProductDto product) {
        if (isBlank(portoneTradeApiSecret)) {
            throw new IllegalStateException("PortOne API credentials are missing.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneTradeApiSecret);

        ResponseEntity<PortOneTradePaymentResponse> response;
        try {
            response = restTemplate.exchange(
                    "https://api.portone.io/payments/" + completeDto.getPaymentId(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PortOneTradePaymentResponse.class
            );
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "PortOne payment lookup failed. status=" + ex.getStatusCode().value()
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("PortOne payment lookup failed.");
        }

        PortOneTradePaymentResponse payment = response.getBody();
        if (payment == null) {
            throw new IllegalStateException("Payment information not found.");
        }

        String paymentId = payment.id;
        String status = payment.status;
        Long amount = payment.amount == null ? null : payment.amount.total;
        String channelKey = payment.channel == null ? null : payment.channel.key;

        if (!completeDto.getPaymentId().equals(paymentId)) {
            throw new IllegalStateException("Payment id does not match.");
        }
        if (!"PAID".equals(status)) {
            throw new IllegalStateException("Payment is not paid.");
        }
        if (amount == null || !amount.equals(product.getPrice())) {
            throw new IllegalStateException("Payment amount does not match.");
        }
        if (!isAllowedTradeChannelKey(channelKey)) {
            throw new IllegalStateException("Payment channel does not match.");
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
            throw new IllegalArgumentException("Payment request body is required.");
        }
        if (completeDto.getProductId() == null) {
            throw new IllegalArgumentException("Product id is required.");
        }
        if (isBlank(completeDto.getPaymentId())) {
            throw new IllegalArgumentException("paymentId is required.");
        }
    }

    private void validateEditableProductStatus(String productStatus) {
        if (!"SALE".equals(productStatus) && !"RESERVE".equals(productStatus)) {
            throw new IllegalArgumentException("Product status can only be changed to SALE or RESERVE.");
        }
    }

    private void validateProductImage(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, and WEBP images can be uploaded.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image size must be 30MB or less.");
        }
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
