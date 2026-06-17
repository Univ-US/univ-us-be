package com.univus.app.reservation.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationCommandServiceImpl implements ReservationCommandService {

    private static final String DEFAULT_SEAT_STATUS = "RESERVED";
    private static final String DEFAULT_ROOM_STATUS = "RESERVED";
    private static final String SEAT_REALTIME_TOPIC = "/sub/reservations/seats";
    private static final String ROOM_REALTIME_TOPIC = "/sub/reservations/rooms";
    private static final String REALTIME_ACTION_RESERVED = "RESERVED";
    private static final String REALTIME_ACTION_CANCELLED = "CANCELLED";
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");
    private static final int PENALTY_BLOCK_THRESHOLD = 5;

    private final ReservationMapper reservationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    @Override
    public ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request) {
        validateReservationPenaltyLimit(memberId);

        int usableSeatCount = reservationMapper.countUsableReadingSeat(request.getSeatId());
        if (usableSeatCount == 0) {
            throw new IllegalArgumentException("사용 가능한 좌석이 아닙니다.");
        }

        int memberOverlapCount = reservationMapper.countOverlappingMemberReadingSeatReservation(
                memberId,
                request.getStartTime(),
                request.getEndTime());
        if (memberOverlapCount > 0) {
            throw new IllegalStateException("같은 시간대에 이미 예약한 좌석이 있습니다.");
        }

        int overlapCount = reservationMapper.countOverlappingReadingSeatReservation(
                request.getSeatId(),
                request.getStartTime(),
                request.getEndTime());
        if (overlapCount > 0) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }

        ReservationDto.ReadingSeatReservationDto reservation =
                ReservationDto.ReadingSeatReservationDto.builder()
                        .memberId(memberId)
                        .seatId(request.getSeatId())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .status(DEFAULT_SEAT_STATUS)
                        .build();

        reservationMapper.insertReadingSeatReservation(reservation);
        ReservationDto.ReadingSeatReservationDto savedReservation =
                reservationMapper.selectReadingSeatReservationForMember(
                        reservation.getReservationId(),
                        memberId);
        ReservationDto.ReadingSeatReservationDto response =
                savedReservation == null ? reservation : savedReservation;
        publishSeatRealtimeEventAfterCommit(REALTIME_ACTION_RESERVED, response);
        return response;
    }

    @Transactional
    @Override
    public void cancelReadingSeatReservation(Long memberId, Long reservationId) {
        ReservationDto.ReadingSeatReservationDto reservation =
                reservationMapper.selectReadingSeatReservationForMember(reservationId, memberId);
        if (reservation == null) {
            throw new IllegalArgumentException("취소할 수 있는 예약을 찾을 수 없습니다.");
        }

        int updated = reservationMapper.cancelReadingSeatReservation(reservationId, memberId);
        if (updated == 0) {
            throw new IllegalArgumentException("취소할 수 있는 예약을 찾을 수 없습니다.");
        }
        publishSeatRealtimeEventAfterCommit(REALTIME_ACTION_CANCELLED, reservation);
    }

    @Transactional
    @Override
    public ReservationDto.RoomReservationDto reserveRoom(
            Long memberId,
            ReservationDto.RoomReservationRequestDto request) {
        validateReservationPenaltyLimit(memberId);

        int usableRoomCount = reservationMapper.countUsableReservationRoom(request.getRoomId());
        if (usableRoomCount == 0) {
            throw new IllegalArgumentException("사용 가능한 공간이 아닙니다.");
        }

        int overlapCount = reservationMapper.countOverlappingRoomReservation(
                request.getRoomId(),
                request.getStartTime(),
                request.getEndTime());
        if (overlapCount > 0) {
            throw new IllegalStateException("이미 예약된 공간입니다.");
        }

        ReservationDto.RoomReservationDto reservation =
                ReservationDto.RoomReservationDto.builder()
                        .memberId(memberId)
                        .roomId(request.getRoomId())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .purpose(request.getPurpose())
                        .status(DEFAULT_ROOM_STATUS)
                        .build();

        reservationMapper.insertRoomReservation(reservation);
        publishRoomRealtimeEventAfterCommit(REALTIME_ACTION_RESERVED, reservation);
        return reservation;
    }

    @Transactional
    @Override
    public void cancelRoomReservation(Long memberId, Long reservationId) {
        ReservationDto.RoomReservationDto reservation =
                reservationMapper.selectRoomReservationForMember(reservationId, memberId);
        if (reservation == null) {
            throw new IllegalArgumentException("취소할 수 있는 공간 예약을 찾을 수 없습니다.");
        }

        int updated = reservationMapper.cancelRoomReservation(reservationId, memberId);
        if (updated == 0) {
            throw new IllegalArgumentException("취소할 수 있는 공간 예약을 찾을 수 없습니다.");
        }
        publishRoomRealtimeEventAfterCommit(REALTIME_ACTION_CANCELLED, reservation);
    }

    @Transactional
    @Override
    public void checkInReadingSeat(Long memberId, Long reservationId) {
        int updated = reservationMapper.checkInReadingSeatReservation(reservationId, memberId);
        if (updated == 0) {
            throw new IllegalArgumentException("입실할 수 있는 예약이 아니거나 이미 처리되었습니다.");
        }
        
        ReservationDto.ReadingSeatReservationDto reservation = 
            reservationMapper.selectReadingSeatReservationForMember(reservationId, memberId);
        publishSeatRealtimeEventAfterCommit("CHECKED_IN", reservation);
    }

    @Transactional
    @Override
    public ReservationDto.ReadingSeatReservationDto extendReadingSeatReservation(Long memberId, Long reservationId) {
        ReservationDto.ReadingSeatReservationDto reservation =
                reservationMapper.selectReadingSeatReservationForMember(reservationId, memberId);

        if (reservation == null || !"USING".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("연장할 수 있는 예약이 아닙니다. (현재 입실하여 사용 중인 좌석만 연장 가능)");
        }

        // 만료 20분 전부터만 연장 가능하도록 검증 로직 추가
        LocalDateTime now = LocalDateTime.now(RESERVATION_ZONE);
        LocalDateTime endTime = reservation.getEndTime();
        Duration timeRemaining = Duration.between(now, endTime);
        
        if (timeRemaining.toMinutes() > 20 || timeRemaining.isNegative()) {
            throw new IllegalStateException("좌석 연장은 만료 20분 전부터 가능합니다.");
        }

        // 최대 예약 가능 시간 상한선 체크 (예: 최초 6시간 + 2회 연장 = 10시간)
        Duration duration = Duration.between(reservation.getStartTime(), reservation.getEndTime());
        if (duration.toHours() >= 10) {
            throw new IllegalStateException("최대 이용 시간(10시간)을 초과하여 더 이상 연장할 수 없습니다.");
        }

        LocalDateTime newEndTime = reservation.getEndTime().plusHours(2);

        int overlapCount = reservationMapper.countOverlappingReadingSeatReservation(
                reservation.getSeatId(),
                reservation.getEndTime(),
                newEndTime);
        
        if (overlapCount > 0) {
            throw new IllegalStateException("해당 시간에 이미 다른 사용자의 예약이 있어 연장할 수 없습니다.");
        }

        int updated = reservationMapper.extendReadingSeatReservation(reservationId, memberId, newEndTime);
        if (updated == 0) {
            throw new IllegalArgumentException("연장 처리에 실패했습니다.");
        }
        
        ReservationDto.ReadingSeatReservationDto updatedReservation =
                reservationMapper.selectReadingSeatReservationForMember(reservationId, memberId);
                
        publishSeatRealtimeEventAfterCommit("EXTENDED", updatedReservation);
        return updatedReservation;
    }

    private void validateReservationPenaltyLimit(Long memberId) {
        int activePenaltyCount = reservationMapper.countActiveReservationPenalties(memberId);
        if (activePenaltyCount >= PENALTY_BLOCK_THRESHOLD) {
            throw new IllegalStateException(
                    "노쇼 패널티가 5회 누적되어 예약이 제한되었습니다. 서약서를 확인하면 다시 예약할 수 있습니다.");
        }
    }

    private void publishSeatRealtimeEventAfterCommit(
            String action,
            ReservationDto.ReadingSeatReservationDto reservation) {
        if (reservation == null) {
            return;
        }

        ReservationDto.ReadingSeatRealtimeEventDto event =
                ReservationDto.ReadingSeatRealtimeEventDto.builder()
                        .action(action)
                        .reservationId(reservation.getReservationId())
                        .memberId(reservation.getMemberId())
                        .seatId(reservation.getSeatId())
                        .readingRoomId(reservation.getReadingRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();
        runAfterCommit(() -> messagingTemplate.convertAndSend(SEAT_REALTIME_TOPIC, event));
    }

    private void publishRoomRealtimeEventAfterCommit(
            String action,
            ReservationDto.RoomReservationDto reservation) {
        if (reservation == null) {
            return;
        }

        ReservationDto.RoomReservationRealtimeEventDto event =
                ReservationDto.RoomReservationRealtimeEventDto.builder()
                        .action(action)
                        .reservationId(reservation.getReservationId())
                        .memberId(reservation.getMemberId())
                        .roomId(reservation.getRoomId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .build();
        runAfterCommit(() -> messagingTemplate.convertAndSend(ROOM_REALTIME_TOPIC, event));
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
