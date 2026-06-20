package com.univus.app.reservation.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
public class SeatChatMessageDto {
    private Long messageId;
    private Long roomId;
    private Long senderReservationId;
    @JsonIgnore
    private Long senderMemberId;
    private String messageText;
    private Integer isRead;
    private LocalDateTime createdAt;
}
