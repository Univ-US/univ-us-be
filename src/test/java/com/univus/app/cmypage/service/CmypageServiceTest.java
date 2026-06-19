package com.univus.app.cmypage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.univus.app.cmypage.dto.CmypageCommentDto;
import com.univus.app.cmypage.mapper.CmypageMapper;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.community.service.PostService;
import com.univus.app.reservation.service.ReservationService;

class CmypageServiceTest {

    private final PostService postService = mock(PostService.class);
    private final CmypageMapper cmypageMapper = mock(CmypageMapper.class);
    private final ReservationService reservationService = mock(ReservationService.class);
    private final CmypageService cmypageService =
            new CmypageService(postService, cmypageMapper, reservationService);

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
