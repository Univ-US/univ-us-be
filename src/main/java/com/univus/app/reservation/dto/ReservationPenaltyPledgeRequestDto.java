package com.univus.app.reservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReservationPenaltyPledgeRequestDto {
    private String pledgeText;
    private Boolean agreed;
}
