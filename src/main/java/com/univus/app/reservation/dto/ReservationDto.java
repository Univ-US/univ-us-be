package com.univus.app.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

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
    public static class ReservationDateOptionsResponseDto {
        private LocalDateTime serverNow;
        private List<ReservationDateOptionDto> dates;
    }

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadingSeatRealtimeEventDto {
        private String action;
        private Long reservationId;
        private Long memberId;
        private Long seatId;
        private Long readingRoomId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoomReservationRequestDto {
        private Long roomId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String purpose;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomAvailabilityDto {
        private Long roomId;
        private String roomName;
        private String roomType;
        private String floorName;
        private String location;
        private Integer capacity;
        private String description;
        private Integer isActive;
        private LocalDateTime createdAt;
        private List<RoomReservationSlotDto> slots;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomReservationSlotDto {
        private Long roomId;
        private Long reservationId;
        private Long reservedMemberId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private Boolean available;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomReservationDto {
        private Long reservationId;
        private Long memberId;
        private Long roomId;
        private String roomName;
        private String roomType;
        private Integer capacity;
        private String purpose;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private LocalDateTime createdAt;
    }
}
