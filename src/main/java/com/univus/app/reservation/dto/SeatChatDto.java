package com.univus.app.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class SeatChatDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActiveSeatReservationDto {
        private Long reservationId;
        private Long memberId;
        private Long seatId;
        private Long readingRoomId;
        private String roomName;
        private String seatNumber;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatChatContextDto {
        private ActiveSeatReservationDto activeReservation;
        private List<SeatChatRoomDto> rooms;
        private Integer totalUnreadCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SeatChatRoomRequestDto {
        private Long targetReservationId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SeatChatMessageRequestDto {
        private String messageText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatChatRoomDto {
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatChatNotificationDto {
        private Long roomId;
        private Long messageId;
        private Long senderReservationId;
        private String senderRoomName;
        private String senderSeatNumber;
        private String messageText;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatChatMessageDto {
        private Long messageId;
        private Long roomId;
        private Long senderReservationId;
        @JsonIgnore
        private Long senderMemberId;
        private String messageText;
        private Integer isRead;
        private LocalDateTime createdAt;
    }
}
