package com.univus.app.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionPlanChangeRequestDto {

    @NotNull
    private Long planId;
}
