package com.univus.app.community.service;

import com.univus.app.community.dto.MarketDto;
import com.univus.app.community.mapper.MarketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketMapper marketMapper;

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
}