package com.univus.app.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDateOptionsResponseDto {
    private LocalDateTime serverNow;
    private List<ReservationDateOptionDto> dates;
}
