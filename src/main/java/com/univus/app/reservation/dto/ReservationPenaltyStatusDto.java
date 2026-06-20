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
public class ReservationPenaltyStatusDto {
    private Integer activePenaltyCount;
    private Integer blockThreshold;
    private Boolean blocked;
    private String pledgePhrase;
    private String message;
}
