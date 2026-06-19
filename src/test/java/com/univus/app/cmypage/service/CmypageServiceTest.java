package com.univus.app.cmypage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.mapper.CmypageMapper;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.community.dto.PostDto;
import com.univus.app.community.dto.PostListResponseDto;
import com.univus.app.community.service.PostListQueryService;

class CmypageServiceTest {

    private final PostListQueryService postListQueryService =
            mock(PostListQueryService.class);
    private final CmypageMapper cmypageMapper = mock(CmypageMapper.class);
    private final CmypageService cmypageService =
            new CmypageServiceImpl(postListQueryService, cmypageMapper);

    @Test
    @DisplayName("내 게시글은 타입이 있는 게시글 목록 응답을 페이지 응답으로 변환한다")
    void getMyPostsUsesTypedPostListResponse() {
        Long memberId = 10L;
        PostDto post = new PostDto();
        PostListResponseDto postListResponse =
                PostListResponseDto.builder()
                        .postList(List.of(post))
                        .totalCount(9)
                        .todayCount(1)
                        .totalPage(2)
                        .currentPage(1)
                        .build();
        when(postListQueryService.getPostList(any(PostDto.class)))
                .thenReturn(postListResponse);

        PaginateUtilRestApiRes<PostDto> result =
                cmypageService.getMyPosts(memberId, 0, 8);

        assertEquals(List.of(post), result.getContent());
        assertEquals(9, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    @DisplayName("댓글 목록은 서버 페이지 정보와 전체 개수를 함께 반환한다")
    void getMyCommentsReturnsPagedResponse() {
        Long memberId = 11L;
        CmypageCommentDto comment = new CmypageCommentDto();
        when(cmypageMapper.selectMyComments(memberId, 8, 8))
                .thenReturn(List.of(comment));
        when(cmypageMapper.countMyComments(memberId)).thenReturn(17);

        PaginateUtilRestApiRes<CmypageCommentDto> result =
                cmypageService.getMyComments(memberId, 1, 8);

        assertEquals(List.of(comment), result.getContent());
        assertEquals(1, result.getPage());
        assertEquals(8, result.getSize());
        assertEquals(17, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        verify(cmypageMapper).selectMyComments(memberId, 8, 8);
    }

    @Test
    @DisplayName("거래 역할 필터는 허용되지 않은 값이면 전체 조회로 정규화한다")
    void getMyTradesNormalizesUnknownRole() {
        Long memberId = 12L;
        when(cmypageMapper.selectMyTrades(memberId, "ALL", 0, 8))
                .thenReturn(List.of());
        when(cmypageMapper.countMyTrades(memberId, "ALL")).thenReturn(0);

        cmypageService.getMyTrades(memberId, "unknown", 0, 8);

        verify(cmypageMapper).selectMyTrades(memberId, "ALL", 0, 8);
        verify(cmypageMapper).countMyTrades(memberId, "ALL");
    }
}
