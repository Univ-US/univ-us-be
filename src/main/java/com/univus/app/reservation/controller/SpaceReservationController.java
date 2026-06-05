package com.univus.app.reservation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univus.app.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @RestController // 💡 React와 JSON 통신을 하려면 RestController 필수!
// @RequestMapping("/api/v1/reservations") // 💡 Base URI 공통화
// @RequiredArgsConstructor
// @Slf4j
/*
public class SpaceReservationController {

    // private final SpaceReservationService reservationService;

    @PostMapping("/spaces/library/seats")
    public ResponseEntity<?> reserveLibrarySeat(@RequestBody ReservationReqDto requestDto,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("도서관 예약 요청 발생 - 유저: {}, 좌석: {}", userDetails.getUsername(), requestDto.getSeatId());
        
        // 여기서 Service 호출해서 Redis 분산 락(Redisson) 태우기!
        // reservationService.reserveLibrarySeat(userDetails.getUsername(), requestDto);
        
        return ResponseEntity.ok().body("도서관 좌석 예약 성공!");
    }


    @PostMapping("/spaces/meeting-rooms")
    public ResponseEntity<?> reserveMeetingRoom(@RequestBody MeetingRoomReqDto requestDto,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("회의실 예약 요청 발생 - 유저: {}, 회의실: {}", userDetails.getUsername(), requestDto.getRoomId());
        
        // reservationService.reserveMeetingRoom(userDetails.getUsername(), requestDto);
        
        return ResponseEntity.ok().body("회의실 예약 성공!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyReservations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // Spring Security JWT 필터가 넣어준 userDetails에서 학번/ID 추출!
        String memberId = userDetails.getUsername(); 
        log.info("내 예약 내역 조회 요청 - 유저: {}", memberId);
        
        // return ResponseEntity.ok(reservationService.getMyReservations(memberId));
        return ResponseEntity.ok().body("내 예약 내역 리스트 리턴 공간");
    }
}
*/