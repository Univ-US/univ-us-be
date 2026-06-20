package com.univus.app.reservation.controller;

import java.util.List;

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

import com.univus.app.reservation.dto.SeatChatContextDto;
import com.univus.app.reservation.dto.SeatChatMessageDto;
import com.univus.app.reservation.dto.SeatChatMessageRequestDto;
import com.univus.app.reservation.dto.SeatChatRoomDto;
import com.univus.app.reservation.dto.SeatChatRoomRequestDto;
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
    public ResponseEntity<SeatChatRoomDto> createOrGetSeatChatRoom(
            @AuthenticationPrincipal Long memberId,
            @RequestBody SeatChatRoomRequestDto request) {
        SeatChatRoomDto room =
                seatChatService.createOrGetSeatChatRoom(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<SeatChatMessageDto>> getSeatChatMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        List<SeatChatMessageDto> messages =
                seatChatService.getSeatChatMessages(memberId, roomId);
        return ResponseEntity.ok(messages);
    }

    @PatchMapping("/{roomId}/read")
    public ResponseEntity<Void> markSeatChatMessagesRead(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId) {
        seatChatService.markSeatChatMessagesRead(memberId, roomId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<SeatChatMessageDto> sendSeatChatMessage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("roomId") Long roomId,
            @RequestBody SeatChatMessageRequestDto request) {
        SeatChatMessageDto message =
                seatChatService.sendSeatChatMessage(memberId, roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
