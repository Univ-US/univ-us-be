package com.univus.app.reservation.dto;

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
public class ReservationDateOptionDto {
    private String key;
    private String date;
    private Integer year;
    private Integer month;
    private Integer day;
    private String dayOfWeek;
    private Boolean today;
    private Boolean sat;
    private Boolean sun;
}
