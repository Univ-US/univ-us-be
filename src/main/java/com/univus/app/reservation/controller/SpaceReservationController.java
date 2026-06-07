package com.univus.app.reservation.controller;

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

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class SpaceReservationController {

    private final ReservationService reservationService;

    @GetMapping("/seats/availability")
    public ResponseEntity<List<ReservationDto.ReadingRoomAvailabilityDto>> getReadingRoomAvailability(
            @RequestParam("startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam("endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        return ResponseEntity.ok(reservationService.getReadingRoomAvailability(startTime, endTime));
    }

    @GetMapping("/seats/availability/{readingRoomId}")
    public ResponseEntity<List<ReservationDto.ReadingSeatAvailabilityDto>> getReadingSeatAvailability(
            @PathVariable("readingRoomId") Long readingRoomId,
            @RequestParam("startTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam("endTime")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {
        return ResponseEntity.ok(
                reservationService.getReadingSeatAvailability(readingRoomId, startTime, endTime));
    }

    @PostMapping("/seats")
    public ResponseEntity<?> reserveReadingSeat(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ReservationDto.ReadingSeatReservationRequestDto request) {
        try {
            ReservationDto.ReadingSeatReservationDto reservation =
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
    public ResponseEntity<List<ReservationDto.ReadingSeatReservationDto>> getMyReadingSeatReservations(
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
}
