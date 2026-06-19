package com.univus.app.reservation.dto;

import java.time.LocalDateTime;

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
public class SeatChatNotificationDto {
    private Long roomId;
    private Long messageId;
    private Long senderReservationId;
    private String senderRoomName;
    private String senderSeatNumber;
    private String messageText;
    private LocalDateTime createdAt;
}
