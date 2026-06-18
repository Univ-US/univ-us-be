package com.univus.app.reservation.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.univus.app.reservation.dto.ReservationDto.ReadingRoomAvailabilityDto;
import com.univus.app.reservation.dto.ReservationDto.ReadingSeatAvailabilityDto;
import com.univus.app.reservation.dto.ReservationDto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReservationDto.ReadingSeatReservationRequestDto;
import com.univus.app.reservation.dto.ReservationDto.ReservationDateOptionsResponseDto;
import com.univus.app.reservation.dto.ReservationDto.ReservationPenaltyPledgeRequestDto;
import com.univus.app.reservation.dto.ReservationDto.ReservationPenaltyStatusDto;
import com.univus.app.reservation.dto.ReservationDto.RoomAvailabilityDto;
import com.univus.app.reservation.dto.ReservationDto.RoomReservationDto;
import com.univus.app.reservation.dto.ReservationDto.RoomReservationRequestDto;
import com.univus.app.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class SpaceReservationController {

    private final ReservationService reservationService;

    @GetMapping("/date-options")
    public ResponseEntity<ReservationDateOptionsResponseDto> getReservationDateOptions(
            @RequestParam(value = "days", defaultValue = "5") int days) {
        return ResponseEntity.ok(reservationService.getReservationDateOptions(days));
    }

    @GetMapping("/penalties/status")
    public ResponseEntity<ReservationPenaltyStatusDto> getReservationPenaltyStatus(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(reservationService.getReservationPenaltyStatus(memberId));
    }

    @PostMapping("/penalties/pledge")
    public ResponseEntity<?> pledgeReservationPenalty(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ReservationPenaltyPledgeRequestDto request) {
        try {
            return ResponseEntity.ok(reservationService.pledgeReservationPenalty(memberId, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/rooms/availability")
    public ResponseEntity<List<RoomAvailabilityDto>> getRoomAvailability(
            @AuthenticationPrincipal Long memberId,
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(reservationService.getRoomAvailability(memberId, date));
    }

    @GetMapping("/rooms/me")
    public ResponseEntity<List<RoomReservationDto>> getMyRoomReservations(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(reservationService.getMyRoomReservations(memberId));
    }

    @PostMapping("/rooms")
    public ResponseEntity<?> reserveRoom(
            @AuthenticationPrincipal Long memberId,
            @RequestBody RoomReservationRequestDto request) {
        try {
            RoomReservationDto reservation = reservationService.reserveRoom(memberId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/rooms/{reservationId}")
    public ResponseEntity<?> cancelRoomReservation(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("reservationId") Long reservationId) {
        try {
            reservationService.cancelRoomReservation(memberId, reservationId);
            return ResponseEntity.ok(Map.of("success", true, "message", "공간 예약이 취소되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/rooms/{reservationId}/checkin")
    public ResponseEntity<?> checkInRoom(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("reservationId") Long reservationId) {
        try {
            reservationService.checkInRoom(memberId, reservationId);
            return ResponseEntity.ok(Map.of("success", true, "message", "정상적으로 입실 처리되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/seats/availability")
    public ResponseEntity<List<ReadingRoomAvailabilityDto>> getReadingRoomAvailability(
            @AuthenticationPrincipal Long memberId,
            @RequestParam("startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam("endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        return ResponseEntity.ok(reservationService.getReadingRoomAvailability(memberId, startTime, endTime));
    }

    @GetMapping("/seats/availability/{readingRoomId}")
    public ResponseEntity<List<ReadingSeatAvailabilityDto>> getReadingSeatAvailability(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("readingRoomId") Long readingRoomId,
            @RequestParam("startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam("endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        return ResponseEntity.ok(
                reservationService.getReadingSeatAvailability(memberId, readingRoomId, startTime, endTime));
    }

    @PostMapping("/seats")
    public ResponseEntity<?> reserveReadingSeat(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ReadingSeatReservationRequestDto request) {
        try {
            ReadingSeatReservationDto reservation =
                    reservationService.reserveReadingSeat(memberId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/seats/me")
    public ResponseEntity<List<ReadingSeatReservationDto>> getMyReadingSeatReservations(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(reservationService.getMyReadingSeatReservations(memberId));
    }

    @DeleteMapping("/seats/{reservationId}")
    public ResponseEntity<?> cancelReadingSeatReservation(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("reservationId") Long reservationId) {
        try {
            reservationService.cancelReadingSeatReservation(memberId, reservationId);
            return ResponseEntity.ok(Map.of("success", true, "message", "예약이 취소되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/seats/{reservationId}/checkin")
    public ResponseEntity<?> checkInReadingSeat(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("reservationId") Long reservationId) {
        try {
            reservationService.checkInReadingSeat(memberId, reservationId);
            return ResponseEntity.ok(Map.of("success", true, "message", "정상적으로 입실 처리되었습니다."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/seats/{reservationId}/extend")
    public ResponseEntity<?> extendReadingSeatReservation(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("reservationId") Long reservationId) {
        try {
            ReadingSeatReservationDto updatedReservation = 
                    reservationService.extendReadingSeatReservation(memberId, reservationId);
            return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "이용 시간이 2시간 연장되었습니다.",
                    "data", updatedReservation));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
}
