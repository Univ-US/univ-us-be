package com.univus.app.reservation.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ReservationDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservationDateOptionDto {
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

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ReadingSeatReservationRequestDto {
        private Long seatId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadingRoomAvailabilityDto {
        private Long readingRoomId;
        private String roomName;
        private String floorName;
        private String description;
        private Integer isActive;
        private LocalDateTime createdAt;
        private Integer totalSeatCount;
        private Integer availableSeatCount;
        private Integer usingSeatCount;
        private Integer disabledSeatCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadingSeatAvailabilityDto {
        private Long seatId;
        private Long readingRoomId;
        private String roomName;
        private String seatNumber;
        private String zoneName;
        private Integer isUsable;
        private String seatStatus;
        private Long reservationId;
        private Long reservedMemberId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadingSeatReservationDto {
        private Long reservationId;
        private Long memberId;
        private Long seatId;
        private Long readingRoomId;
        private String roomName;
        private String seatNumber;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private LocalDateTime createdAt;
    }
}
