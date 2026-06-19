package com.univus.app.reservation.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.univus.app.reservation.dto.ReservationDto;

@Component
public class ReservationPolicy {

    static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");
    static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    static final int SLOT_HOURS = 2;
    static final int CHECK_IN_WINDOW_MINUTES = 20;
    static final int MAX_SEAT_RESERVATION_HOURS = 6;
    static final int MAX_SEAT_USAGE_HOURS = 10;
    static final int EXTENSION_HOURS = 2;
    static final int EXTENSION_WINDOW_MINUTES = 20;
    static final int PENALTY_BLOCK_THRESHOLD = 5;
    static final String PENALTY_PLEDGE_PHRASE =
            "예약한 시설은 책임 있게 이용하며, 사전 취소 없이 이용하지 않는 일이 반복되지 않도록 유의하겠습니다.";
    static final String PENALTY_BLOCK_MESSAGE =
            "노쇼 패널티가 5회 누적되어 예약이 제한되었습니다. 서약서를 확인하면 다시 예약할 수 있습니다.";
    static final String PENALTY_AVAILABLE_MESSAGE = "예약 이용이 가능합니다.";

    public void requireMember(Long memberId) {
        Assert.notNull(memberId, "로그인이 필요합니다.");
    }

    public void requireReadingRoom(Long readingRoomId) {
        Assert.notNull(readingRoomId, "독서실 ID는 필수입니다.");
    }

    public void requireReservationId(Long reservationId) {
        Assert.notNull(reservationId, "예약 ID는 필수입니다.");
    }

    public void requireReservationDate(LocalDate date) {
        Assert.notNull(date, "예약 날짜는 필수입니다.");
    }

    public ReservationDto.ReadingSeatReservationDto requireSeatReservation(
            ReservationDto.ReadingSeatReservationDto reservation,
            String message) {
        return Optional.ofNullable(reservation)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    public ReservationDto.RoomReservationDto requireRoomReservation(
            ReservationDto.RoomReservationDto reservation,
            String message) {
        return Optional.ofNullable(reservation)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    public void validateSeatReservationRequest(
            ReservationDto.ReadingSeatReservationRequestDto request) {
        Assert.notNull(request, "예약 요청 본문은 필수입니다.");
        Assert.notNull(request.getSeatId(), "좌석 ID는 필수입니다.");
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateReservationTimePolicy(
                request.getStartTime(),
                request.getEndTime(),
                MAX_SEAT_RESERVATION_HOURS);
    }

    public void validateRoomReservationRequest(
            ReservationDto.RoomReservationRequestDto request) {
        Assert.notNull(request, "예약 요청 본문은 필수입니다.");
        Assert.notNull(request.getRoomId(), "공간 ID는 필수입니다.");
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateReservationTimePolicy(request.getStartTime(), request.getEndTime(), null);
        requireRoomReservationWindowOpen(request.getStartTime());
    }

    public void validateSeatAvailabilityRange(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        validateReservationTimePolicy(startTime, endTime, MAX_SEAT_RESERVATION_HOURS);
    }

    public void validatePenaltyPledge(
            ReservationDto.ReservationPenaltyPledgeRequestDto request,
            int activePenaltyCount) {
        Assert.notNull(request, "서약 요청 본문은 필수입니다.");
        Assert.isTrue(
                Boolean.TRUE.equals(request.getAgreed()),
                "예약 이용 정책 확인에 동의해주세요.");

        String pledgeText = Optional.ofNullable(request.getPledgeText())
                .map(String::trim)
                .orElse("");
        Assert.isTrue(
                PENALTY_PLEDGE_PHRASE.equals(pledgeText),
                "서약 문구를 정확히 입력해주세요.");
        Assert.state(
                activePenaltyCount >= PENALTY_BLOCK_THRESHOLD,
                "서약은 노쇼 패널티 5회 이상인 경우에만 진행할 수 있습니다.");
    }

    public void requireCancelableStatus(String status) {
        Assert.isTrue(
                "RESERVED".equals(status) || "USING".equals(status),
                "이미 취소되었거나 완료된 예약입니다.");
    }

    public void validateSeatCheckIn(
            ReservationDto.ReadingSeatReservationDto reservation) {
        Assert.isTrue(
                "RESERVED".equals(reservation.getStatus()),
                "입실할 수 있는 예약 상태가 아닙니다.");
        requireReservationTimes(reservation.getStartTime(), reservation.getEndTime());

        LocalDateTime now = now();
        Assert.isTrue(
                !now.isBefore(reservation.getStartTime()),
                "예약 시작 시간 이후 입실할 수 있습니다.");
        Assert.isTrue(
                now.isBefore(reservation.getEndTime()),
                "이미 종료된 예약입니다.");
        Assert.isTrue(
                now.isBefore(seatCheckInDeadline(reservation)),
                "입실 가능 시간이 지나 노쇼 처리 대상입니다. 예약 내역을 새로고침해주세요.");
    }

    public void validateRoomCheckIn(
            ReservationDto.RoomReservationDto reservation) {
        Assert.isTrue(
                "RESERVED".equals(reservation.getStatus()),
                "입실할 수 있는 공간 예약 상태가 아닙니다.");
        requireReservationTimes(reservation.getStartTime(), reservation.getEndTime());

        LocalDateTime now = now();
        Assert.isTrue(
                !now.isBefore(reservation.getStartTime()),
                "예약 시작 시간 이후 입실할 수 있습니다.");
        Assert.isTrue(
                now.isBefore(reservation.getEndTime()),
                "이미 종료된 예약입니다.");
        Assert.isTrue(
                now.isBefore(reservation.getStartTime().plusMinutes(CHECK_IN_WINDOW_MINUTES)),
                "입실 가능 시간이 지나 노쇼 처리 대상입니다. 예약 내역을 새로고침해주세요.");
    }

    public void validateRoomCancellation(
            ReservationDto.RoomReservationDto reservation) {
        requireCancelableStatus(reservation.getStatus());

        LocalDateTime now = now();
        Assert.isTrue(now.isBefore(reservation.getEndTime()), "이미 종료된 예약입니다.");
        Assert.isTrue(
                !"RESERVED".equals(reservation.getStatus())
                        || now.isBefore(
                                reservation.getStartTime()
                                        .plusMinutes(CHECK_IN_WINDOW_MINUTES)),
                "입실 가능 시간이 지나 노쇼 처리 대상입니다. 예약 내역을 새로고침해주세요.");
    }

    public void requirePenaltyAvailable(int activePenaltyCount) {
        Assert.state(
                activePenaltyCount < PENALTY_BLOCK_THRESHOLD,
                PENALTY_BLOCK_MESSAGE);
    }

    public void requireUsableSeat(int usableSeatCount) {
        Assert.isTrue(usableSeatCount > 0, "사용 가능한 좌석이 아닙니다.");
    }

    public void requireNoMemberSeatOverlap(int overlapCount) {
        Assert.state(overlapCount == 0, "같은 시간대에 이미 예약한 좌석이 있습니다.");
    }

    public void requireNoSeatOverlap(int overlapCount) {
        Assert.state(overlapCount == 0, "이미 예약된 좌석입니다.");
    }

    public void requireUsableRoom(int usableRoomCount) {
        Assert.isTrue(usableRoomCount > 0, "사용 가능한 공간이 아닙니다.");
    }

    public void requireNoRoomOverlap(int overlapCount) {
        Assert.state(overlapCount == 0, "이미 예약된 공간입니다.");
    }

    public void requireUpdated(int updated, String message) {
        Assert.isTrue(updated > 0, message);
    }

    public ReservationDto.ReadingSeatReservationDto requireExtendableSeat(
            ReservationDto.ReadingSeatReservationDto reservation) {
        ReservationDto.ReadingSeatReservationDto required = requireSeatReservation(
                reservation,
                "연장할 수 있는 예약이 아닙니다. (현재 입실하여 사용 중인 좌석만 연장 가능)");
        Assert.isTrue(
                "USING".equals(required.getStatus()),
                "연장할 수 있는 예약이 아닙니다. (현재 입실하여 사용 중인 좌석만 연장 가능)");
        return required;
    }

    public LocalDateTime calculateExtendedEndTime(
            ReservationDto.ReadingSeatReservationDto reservation) {
        Duration remaining = Duration.between(now(), reservation.getEndTime());
        Assert.state(
                !remaining.isNegative()
                        && remaining.toMinutes() <= EXTENSION_WINDOW_MINUTES,
                "좌석 연장은 만료 20분 전부터 가능합니다.");

        Duration usage = Duration.between(
                reservation.getStartTime(),
                reservation.getEndTime());
        Assert.state(
                usage.toHours() < MAX_SEAT_USAGE_HOURS,
                "최대 이용 시간(10시간)을 초과하여 더 이상 연장할 수 없습니다.");
        return reservation.getEndTime().plusHours(EXTENSION_HOURS);
    }

    public void requireExtensionSlotAvailable(int overlapCount) {
        Assert.state(
                overlapCount == 0,
                "해당 시간에 이미 다른 사용자의 예약이 있어 연장할 수 없습니다.");
    }

    public ReservationDto.ReservationPenaltyStatusDto buildPenaltyStatus(
            int activePenaltyCount) {
        boolean blocked = activePenaltyCount >= PENALTY_BLOCK_THRESHOLD;
        return ReservationDto.ReservationPenaltyStatusDto.builder()
                .activePenaltyCount(activePenaltyCount)
                .blockThreshold(PENALTY_BLOCK_THRESHOLD)
                .blocked(blocked)
                .pledgePhrase(PENALTY_PLEDGE_PHRASE)
                .message(blocked ? PENALTY_BLOCK_MESSAGE : PENALTY_AVAILABLE_MESSAGE)
                .build();
    }

    private void validateTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        Assert.notNull(startTime, "시작 시간과 종료 시간은 필수입니다.");
        Assert.notNull(endTime, "시작 시간과 종료 시간은 필수입니다.");
        Assert.isTrue(
                endTime.isAfter(startTime),
                "종료 시간은 시작 시간보다 뒤여야 합니다.");
    }

    private void validateReservationTimePolicy(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer maxReservationHours) {
        Assert.isTrue(
                isSlotBoundary(startTime) && isSlotBoundary(endTime),
                "예약 시간은 짝수 시간대의 2시간 단위로 선택해야 합니다.");
        Assert.isTrue(
                endTime.isAfter(now()),
                "이미 종료된 예약 시간입니다.");

        LocalDateTime closeTime = startTime.toLocalDate().plusDays(1).atStartOfDay();
        Assert.isTrue(
                !startTime.toLocalTime().isBefore(OPEN_TIME)
                        && !endTime.isAfter(closeTime),
                "예약 가능 시간은 08:00부터 24:00까지입니다.");
        Assert.isTrue(
                endTime.toLocalDate().equals(startTime.toLocalDate())
                        || endTime.equals(closeTime),
                "예약은 하루 운영 시간 안에서만 가능합니다.");

        long reservationMinutes = Duration.between(startTime, endTime).toMinutes();
        long slotMinutes = SLOT_HOURS * 60L;
        Assert.isTrue(
                reservationMinutes >= slotMinutes,
                "예약은 최소 2시간부터 가능합니다.");
        Assert.isTrue(
                maxReservationHours == null
                        || reservationMinutes <= maxReservationHours * 60L,
                "예약은 최대 6시간까지 가능합니다.");
        Assert.isTrue(
                reservationMinutes % slotMinutes == 0,
                "예약 시간은 2시간 단위여야 합니다.");
    }

    public void requireRoomReservationWindowOpen(LocalDateTime startTime) {
        Assert.isTrue(
                now().isBefore(startTime.plusMinutes(CHECK_IN_WINDOW_MINUTES)),
                "예약 시작 후 20분이 지난 시간대는 예약할 수 없습니다.");
    }

    private void requireReservationTimes(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        Assert.notNull(startTime, "예약 시간 정보를 확인할 수 없습니다.");
        Assert.notNull(endTime, "예약 시간 정보를 확인할 수 없습니다.");
    }

    private LocalDateTime seatCheckInDeadline(
            ReservationDto.ReadingSeatReservationDto reservation) {
        LocalDateTime checkInStart = Optional.ofNullable(reservation.getCreatedAt())
                .filter(createdAt -> createdAt.isAfter(reservation.getStartTime()))
                .orElse(reservation.getStartTime());
        LocalDateTime deadline = checkInStart.plusMinutes(CHECK_IN_WINDOW_MINUTES);
        return deadline.isBefore(reservation.getEndTime())
                ? deadline
                : reservation.getEndTime();
    }

    private boolean isSlotBoundary(LocalDateTime dateTime) {
        return dateTime.getMinute() == 0
                && dateTime.getSecond() == 0
                && dateTime.getNano() == 0
                && dateTime.getHour() % SLOT_HOURS == 0;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(RESERVATION_ZONE);
    }
}
