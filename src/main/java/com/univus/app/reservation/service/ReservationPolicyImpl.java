package com.univus.app.reservation.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.univus.app.exception.ConflictException;
import com.univus.app.exception.InvalidRequestException;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReadingSeatReservationRequestDto;
import com.univus.app.reservation.dto.ReservationPenaltyPledgeRequestDto;
import com.univus.app.reservation.dto.ReservationPenaltyStatusDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationRequestDto;

@Service
public class ReservationPolicyImpl implements ReservationPolicy {

    public void requireMember(Long memberId) {
        if (memberId == null) {
            throw new InvalidRequestException("로그인이 필요합니다.");
        }
    }

    public void requireReadingRoom(Long readingRoomId) {
        if (readingRoomId == null) {
            throw new InvalidRequestException("독서실 ID는 필수입니다.");
        }
    }

    public void requireReservationId(Long reservationId) {
        if (reservationId == null) {
            throw new InvalidRequestException("예약 ID는 필수입니다.");
        }
    }

    public void requireReservationDate(LocalDate date) {
        if (date == null) {
            throw new InvalidRequestException("예약 날짜는 필수입니다.");
        }
    }

    public ReadingSeatReservationDto requireSeatReservation(
            ReadingSeatReservationDto reservation,
            String message) {
        if (reservation == null) {
            throw new InvalidRequestException(message);
        }
        return reservation;
    }

    public RoomReservationDto requireRoomReservation(
            RoomReservationDto reservation,
            String message) {
        if (reservation == null) {
            throw new InvalidRequestException(message);
        }
        return reservation;
    }

    public void validateSeatReservationRequest(
            ReadingSeatReservationRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("예약 요청 본문은 필수입니다.");
        }
        if (request.getSeatId() == null) {
            throw new InvalidRequestException("좌석 ID는 필수입니다.");
        }
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateReservationTimePolicy(
                request.getStartTime(),
                request.getEndTime(),
                ReservationConstants.MAX_SEAT_RESERVATION_HOURS);
    }

    public void validateRoomReservationRequest(
            RoomReservationRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("예약 요청 본문은 필수입니다.");
        }
        if (request.getRoomId() == null) {
            throw new InvalidRequestException("공간 ID는 필수입니다.");
        }
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateReservationTimePolicy(request.getStartTime(), request.getEndTime(), null);
        requireRoomReservationWindowOpen(request.getStartTime());
    }

    public void validateSeatAvailabilityRange(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        validateReservationTimePolicy(
                startTime,
                endTime,
                ReservationConstants.MAX_SEAT_RESERVATION_HOURS);
    }

    public void validatePenaltyPledge(
            ReservationPenaltyPledgeRequestDto request,
            int activePenaltyCount) {
        if (request == null) {
            throw new InvalidRequestException("서약 요청 본문은 필수입니다.");
        }
        if (!Boolean.TRUE.equals(request.getAgreed())) {
            throw new InvalidRequestException("예약 이용 정책 확인에 동의해주세요.");
        }

        String pledgeText = request.getPledgeText();
        if (pledgeText == null) {
            pledgeText = "";
        } else {
            pledgeText = pledgeText.trim();
        }

        if (!ReservationConstants.PENALTY_PLEDGE_PHRASE.equals(pledgeText)) {
            throw new InvalidRequestException("서약 문구를 정확히 입력해주세요.");
        }
        if (activePenaltyCount < ReservationConstants.PENALTY_BLOCK_THRESHOLD) {
            throw new ConflictException(
                    "서약은 노쇼 패널티 5회 이상인 경우에만 진행할 수 있습니다.");
        }
    }

    public void requireCancelableStatus(String status) {
        boolean cancelable =
                ReservationConstants.STATUS_RESERVED.equals(status)
                        || ReservationConstants.STATUS_USING.equals(status);
        if (!cancelable) {
            throw new InvalidRequestException("이미 취소되었거나 완료된 예약입니다.");
        }
    }

    public void validateSeatCheckIn(
            ReadingSeatReservationDto reservation) {
        if (!ReservationConstants.STATUS_RESERVED.equals(
                reservation.getStatus())) {
            throw new InvalidRequestException("입실할 수 있는 예약 상태가 아닙니다.");
        }
        requireReservationTimes(reservation.getStartTime(), reservation.getEndTime());

        LocalDateTime now = now();
        if (now.isBefore(reservation.getStartTime())) {
            throw new InvalidRequestException("예약 시작 시간 이후 입실할 수 있습니다.");
        }
        if (!now.isBefore(reservation.getEndTime())) {
            throw new InvalidRequestException("이미 종료된 예약입니다.");
        }
        if (!now.isBefore(seatCheckInDeadline(reservation))) {
            throw new InvalidRequestException(
                    "입실 가능 시간이 지나 노쇼 처리 대상입니다. 예약 내역을 새로고침해주세요.");
        }
    }

    public void validateRoomCheckIn(
            RoomReservationDto reservation) {
        if (!ReservationConstants.STATUS_RESERVED.equals(
                reservation.getStatus())) {
            throw new InvalidRequestException(
                    "입실할 수 있는 공간 예약 상태가 아닙니다.");
        }
        requireReservationTimes(reservation.getStartTime(), reservation.getEndTime());

        LocalDateTime now = now();
        if (now.isBefore(reservation.getStartTime())) {
            throw new InvalidRequestException("예약 시작 시간 이후 입실할 수 있습니다.");
        }
        if (!now.isBefore(reservation.getEndTime())) {
            throw new InvalidRequestException("이미 종료된 예약입니다.");
        }

        LocalDateTime checkInDeadline = reservation.getStartTime()
                .plusMinutes(ReservationConstants.CHECK_IN_WINDOW_MINUTES);
        if (!now.isBefore(checkInDeadline)) {
            throw new InvalidRequestException(
                    "입실 가능 시간이 지나 노쇼 처리 대상입니다. 예약 내역을 새로고침해주세요.");
        }
    }

    public void validateRoomCancellation(
            RoomReservationDto reservation) {
        requireCancelableStatus(reservation.getStatus());

        LocalDateTime now = now();
        if (!now.isBefore(reservation.getEndTime())) {
            throw new InvalidRequestException("이미 종료된 예약입니다.");
        }

        LocalDateTime checkInDeadline = reservation.getStartTime()
                .plusMinutes(ReservationConstants.CHECK_IN_WINDOW_MINUTES);
        boolean noShowTarget =
                ReservationConstants.STATUS_RESERVED.equals(
                        reservation.getStatus())
                        && !now.isBefore(checkInDeadline);
        if (noShowTarget) {
            throw new InvalidRequestException(
                    "입실 가능 시간이 지나 노쇼 처리 대상입니다. 예약 내역을 새로고침해주세요.");
        }
    }

    public void requirePenaltyAvailable(int activePenaltyCount) {
        if (activePenaltyCount >= ReservationConstants.PENALTY_BLOCK_THRESHOLD) {
            throw new ConflictException(
                    ReservationConstants.PENALTY_BLOCK_MESSAGE);
        }
    }

    public void requireUsableSeat(int usableSeatCount) {
        if (usableSeatCount <= 0) {
            throw new InvalidRequestException("사용 가능한 좌석이 아닙니다.");
        }
    }

    public void requireNoMemberSeatOverlap(int overlapCount) {
        if (overlapCount != 0) {
            throw new ConflictException(
                    "같은 시간대에 이미 예약한 좌석이 있습니다.");
        }
    }

    public void requireNoSeatOverlap(int overlapCount) {
        if (overlapCount != 0) {
            throw new ConflictException("이미 예약된 좌석입니다.");
        }
    }

    public void requireUsableRoom(int usableRoomCount) {
        if (usableRoomCount <= 0) {
            throw new InvalidRequestException("사용 가능한 공간이 아닙니다.");
        }
    }

    public void requireNoRoomOverlap(int overlapCount) {
        if (overlapCount != 0) {
            throw new ConflictException("이미 예약된 공간입니다.");
        }
    }

    public void requireUpdated(int updated, String message) {
        if (updated <= 0) {
            throw new InvalidRequestException(message);
        }
    }

    public ReadingSeatReservationDto requireExtendableSeat(
            ReadingSeatReservationDto reservation) {
        ReadingSeatReservationDto required = requireSeatReservation(
                reservation,
                "연장할 수 있는 예약이 아닙니다. (현재 입실하여 사용 중인 좌석만 연장 가능)");
        if (!ReservationConstants.STATUS_USING.equals(required.getStatus())) {
            throw new InvalidRequestException(
                    "연장할 수 있는 예약이 아닙니다. (현재 입실하여 사용 중인 좌석만 연장 가능)");
        }
        return required;
    }

    public LocalDateTime calculateExtendedEndTime(
            ReadingSeatReservationDto reservation) {
        Duration remaining = Duration.between(now(), reservation.getEndTime());
        boolean extensionTime =
                !remaining.isNegative()
                        && remaining.toMinutes()
                                <= ReservationConstants.EXTENSION_WINDOW_MINUTES;
        if (!extensionTime) {
            throw new ConflictException(
                    "좌석 연장은 만료 20분 전부터 가능합니다.");
        }

        Duration usage = Duration.between(
                reservation.getStartTime(),
                reservation.getEndTime());
        if (usage.toHours() >= ReservationConstants.MAX_SEAT_USAGE_HOURS) {
            throw new ConflictException(
                    "최대 이용 시간(10시간)을 초과하여 더 이상 연장할 수 없습니다.");
        }
        return reservation.getEndTime()
                .plusHours(ReservationConstants.EXTENSION_HOURS);
    }

    public void requireExtensionSlotAvailable(int overlapCount) {
        if (overlapCount != 0) {
            throw new ConflictException(
                    "해당 시간에 이미 다른 사용자의 예약이 있어 연장할 수 없습니다.");
        }
    }

    public ReservationPenaltyStatusDto buildPenaltyStatus(
            int activePenaltyCount) {
        boolean blocked =
                activePenaltyCount
                        >= ReservationConstants.PENALTY_BLOCK_THRESHOLD;
        String message = ReservationConstants.PENALTY_AVAILABLE_MESSAGE;
        if (blocked) {
            message = ReservationConstants.PENALTY_BLOCK_MESSAGE;
        }

        return ReservationPenaltyStatusDto.builder()
                .activePenaltyCount(activePenaltyCount)
                .blockThreshold(ReservationConstants.PENALTY_BLOCK_THRESHOLD)
                .blocked(blocked)
                .pledgePhrase(ReservationConstants.PENALTY_PLEDGE_PHRASE)
                .message(message)
                .build();
    }

    private void validateTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new InvalidRequestException(
                    "시작 시간과 종료 시간은 필수입니다.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidRequestException(
                    "종료 시간은 시작 시간보다 뒤여야 합니다.");
        }
    }

    private void validateReservationTimePolicy(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer maxReservationHours) {
        if (!isSlotBoundary(startTime) || !isSlotBoundary(endTime)) {
            throw new InvalidRequestException(
                    "예약 시간은 짝수 시간대의 2시간 단위로 선택해야 합니다.");
        }
        if (!endTime.isAfter(now())) {
            throw new InvalidRequestException("이미 종료된 예약 시간입니다.");
        }

        LocalDateTime closeTime = startTime.toLocalDate().plusDays(1).atStartOfDay();
        boolean insideOperatingHours =
                !startTime.toLocalTime().isBefore(ReservationConstants.OPEN_TIME)
                        && !endTime.isAfter(closeTime);
        if (!insideOperatingHours) {
            throw new InvalidRequestException(
                    "예약 가능 시간은 08:00부터 24:00까지입니다.");
        }

        boolean sameOperatingDay =
                endTime.toLocalDate().equals(startTime.toLocalDate())
                        || endTime.equals(closeTime);
        if (!sameOperatingDay) {
            throw new InvalidRequestException(
                    "예약은 하루 운영 시간 안에서만 가능합니다.");
        }

        long reservationMinutes = Duration.between(startTime, endTime).toMinutes();
        long slotMinutes = ReservationConstants.SLOT_HOURS * 60L;
        if (reservationMinutes < slotMinutes) {
            throw new InvalidRequestException(
                    "예약은 최소 2시간부터 가능합니다.");
        }
        if (maxReservationHours != null
                && reservationMinutes > maxReservationHours * 60L) {
            throw new InvalidRequestException(
                    "예약은 최대 6시간까지 가능합니다.");
        }
        if (reservationMinutes % slotMinutes != 0) {
            throw new InvalidRequestException(
                    "예약 시간은 2시간 단위여야 합니다.");
        }
    }

    public void requireRoomReservationWindowOpen(LocalDateTime startTime) {
        LocalDateTime reservationDeadline = startTime.plusMinutes(
                ReservationConstants.CHECK_IN_WINDOW_MINUTES);
        if (!now().isBefore(reservationDeadline)) {
            throw new InvalidRequestException(
                    "예약 시작 후 20분이 지난 시간대는 예약할 수 없습니다.");
        }
    }

    private void requireReservationTimes(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new InvalidRequestException(
                    "예약 시간 정보를 확인할 수 없습니다.");
        }
    }

    private LocalDateTime seatCheckInDeadline(
            ReadingSeatReservationDto reservation) {
        LocalDateTime checkInStart = reservation.getStartTime();
        LocalDateTime createdAt = reservation.getCreatedAt();
        if (createdAt != null && createdAt.isAfter(checkInStart)) {
            checkInStart = createdAt;
        }

        LocalDateTime deadline = checkInStart.plusMinutes(
                ReservationConstants.CHECK_IN_WINDOW_MINUTES);
        if (deadline.isBefore(reservation.getEndTime())) {
            return deadline;
        }
        return reservation.getEndTime();
    }

    private boolean isSlotBoundary(LocalDateTime dateTime) {
        return dateTime.getMinute() == 0
                && dateTime.getSecond() == 0
                && dateTime.getNano() == 0
                && dateTime.getHour() % ReservationConstants.SLOT_HOURS == 0;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ReservationConstants.RESERVATION_ZONE);
    }
}
