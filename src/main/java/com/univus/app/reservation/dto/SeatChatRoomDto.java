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
public class SeatChatRoomDto {
    private Long roomId;
    private Long myReservationId;
    private Long targetReservationId;
    private Long targetSeatId;
    private Long targetReadingRoomId;
    private String targetRoomName;
    private String targetSeatNumber;
    private String status;
    private LocalDateTime createdAt;
    private String lastMessageText;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
}
