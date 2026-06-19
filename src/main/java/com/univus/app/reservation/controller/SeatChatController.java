package com.univus.app.reservation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univus.app.reservation.dto.SeatChatDto.SeatChatContextDto;
import com.univus.app.reservation.dto.SeatChatDto.SeatChatMessageDto;
import com.univus.app.reservation.dto.SeatChatDto.SeatChatMessageRequestDto;
import com.univus.app.reservation.dto.SeatChatDto.SeatChatRoomDto;
import com.univus.app.reservation.dto.SeatChatDto.SeatChatRoomRequestDto;
import com.univus.app.reservation.service.SeatChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations/seat-chats")
@RequiredArgsConstructor
public class SeatChatController {

    private final SeatChatService seatChatService;

    @GetMapping
    public ResponseEntity<SeatChatContextDto> getSeatChatContext(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(seatChatService.getSeatChatContext(memberId));
    }

    @PostMapping
    public ResponseEntity<?> createOrGetSeatChatRoom(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SeatChatRoomRequestDto request) {
        try {
            SeatChatRoomDto room = seatChatService.createOrGetSeatChatRoom(memberId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(room);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<?> getSeatChatMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        try {
            List<SeatChatMessageDto> messages =
                    seatChatService.getSeatChatMessages(memberId, roomId);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PatchMapping("/{roomId}/read")
    public ResponseEntity<?> markSeatChatMessagesRead(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        try {
            seatChatService.markSeatChatMessagesRead(memberId, roomId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<?> sendSeatChatMessage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId,
            @RequestBody SeatChatMessageRequestDto request) {
        try {
            SeatChatMessageDto message =
                    seatChatService.sendSeatChatMessage(memberId, roomId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
}
