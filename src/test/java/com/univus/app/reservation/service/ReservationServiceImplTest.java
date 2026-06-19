package com.univus.app.reservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

class ReservationServiceImplTest {

    private final ReservationMapper reservationMapper = mock(ReservationMapper.class);
    private final ReservationCommandService reservationCommandService =
            mock(ReservationCommandService.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final ReservationServiceImpl reservationService =
            new ReservationServiceImpl(
                    reservationMapper,
                    reservationCommandService,
                    redissonClient);

    @Test
    @DisplayName("패널티 이력은 발생 최신순 페이지와 전체 개수를 반환한다")
    void getReservationPenaltyHistoryReturnsPagedResponse() {
        Long memberId = 21L;
        ReservationDto.ReservationPenaltyHistoryDto history =
                ReservationDto.ReservationPenaltyHistoryDto.builder()
                        .penaltyId(31L)
                        .penaltyType("NO_SHOW")
                        .reason("회의실 노쇼")
                        .status("PLEDGED")
                        .createdAt(LocalDateTime.of(2026, 6, 18, 10, 20))
                        .resolvedAt(LocalDateTime.of(2026, 6, 19, 9, 0))
                        .build();
        when(reservationMapper.selectReservationPenaltyHistory(memberId, 5, 5))
                .thenReturn(List.of(history));
        when(reservationMapper.countReservationPenaltyHistory(memberId))
                .thenReturn(7);

        PaginateUtilRestApiRes<ReservationDto.ReservationPenaltyHistoryDto> result =
                reservationService.getReservationPenaltyHistory(memberId, 1, 5);

        assertEquals(List.of(history), result.getContent());
        assertEquals(1, result.getPage());
        assertEquals(7, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        verify(reservationMapper).selectReservationPenaltyHistory(memberId, 5, 5);
    }
}
