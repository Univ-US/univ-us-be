package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final List<String> DAY_OF_WEEK_LABELS =
            List.of("일", "월", "화", "수", "목", "금", "토");
    private static final int MIN_DATE_OPTION_DAYS = 1;
    private static final int MAX_DATE_OPTION_DAYS = 14;

    private final ReservationMapper reservationMapper;
    private final ReservationCommandService reservationCommandService;
    private final ReservationPolicy reservationPolicy;
    private final ReservationLockExecutor lockExecutor;
    private final RoomSlotFactory roomSlotFactory;

    @Override
    public ReservationDto.ReservationDateOptionsResponseDto getReservationDateOptions(
            int days) {
        LocalDateTime serverNow =
                LocalDateTime.now(ReservationPolicy.RESERVATION_ZONE);
        LocalDate today = serverNow.toLocalDate();
        List<ReservationDto.ReservationDateOptionDto> dates =
                IntStream.range(0, normalizeDateOptionDays(days))
                        .mapToObj(index -> createDateOption(today, index))
                        .toList();

        return ReservationDto.ReservationDateOptionsResponseDto.builder()
                .serverNow(serverNow)
                .dates(dates)
                .build();
    }

    @Override
    public ReservationDto.ReservationPenaltyStatusDto getReservationPenaltyStatus(
            Long memberId) {
        reservationPolicy.requireMember(memberId);
        return reservationPolicy.buildPenaltyStatus(
                reservationMapper.countActiveReservationPenalties(memberId));
    }

    @Override
    public PaginateUtilRestApiRes<ReservationDto.ReservationPenaltyHistoryDto>
            getReservationPenaltyHistory(
                    Long memberId,
                    Integer page,
                    Integer size) {
        reservationPolicy.requireMember(memberId);
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        List<ReservationDto.ReservationPenaltyHistoryDto> history =
                reservationMapper.selectReservationPenaltyHistory(
                        memberId,
                        PaginateUtilRestApi.offset(safePage, safeSize),
                        safeSize);

        return PaginateUtilRestApi.of(
                history,
                reservationMapper.countReservationPenaltyHistory(memberId),
                safePage,
                safeSize);
    }

    @Transactional
    @Override
    public ReservationDto.ReservationPenaltyStatusDto pledgeReservationPenalty(
            Long memberId,
            ReservationDto.ReservationPenaltyPledgeRequestDto request) {
        reservationPolicy.requireMember(memberId);
        int activePenaltyCount =
                reservationMapper.countActiveReservationPenalties(memberId);
        reservationPolicy.validatePenaltyPledge(request, activePenaltyCount);

        reservationMapper.pledgeActiveReservationPenalties(memberId);
        return reservationPolicy.buildPenaltyStatus(
                reservationMapper.countActiveReservationPenalties(memberId));
    }

    @Override
    public List<ReservationDto.ReadingRoomAvailabilityDto>
            getReadingRoomAvailability(
                    Long memberId,
                    LocalDateTime startTime,
                    LocalDateTime endTime) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.validateSeatAvailabilityRange(startTime, endTime);
        return reservationMapper.selectReadingRoomAvailability(
                memberId,
                startTime,
                endTime);
    }

    @Override
    public List<ReservationDto.ReadingSeatAvailabilityDto>
            getReadingSeatAvailability(
                    Long memberId,
                    Long readingRoomId,
                    LocalDateTime startTime,
                    LocalDateTime endTime) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReadingRoom(readingRoomId);
        reservationPolicy.validateSeatAvailabilityRange(startTime, endTime);
        return reservationMapper.selectReadingSeatAvailability(
                memberId,
                readingRoomId,
                startTime,
                endTime);
    }

    @Override
    public ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.validateSeatReservationRequest(request);

        return lockExecutor.withSeatLocks(
                memberId,
                request.getSeatId(),
                () -> reservationCommandService.reserveReadingSeat(
                        memberId,
                        request));
    }

    @Override
    public List<ReservationDto.ReadingSeatReservationDto>
            getMyReadingSeatReservations(Long memberId) {
        reservationPolicy.requireMember(memberId);
        return reservationMapper.selectMyReadingSeatReservations(memberId);
    }

    @Override
    public void cancelReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationId(reservationId);

        ReservationDto.ReadingSeatReservationDto reservation =
                reservationPolicy.requireSeatReservation(
                        reservationMapper.selectReadingSeatReservationForMember(
                                reservationId,
                                memberId),
                        "취소할 수 있는 예약을 찾을 수 없습니다.");
        reservationPolicy.requireCancelableStatus(reservation.getStatus());

        lockExecutor.withSeatLocks(
                memberId,
                reservation.getSeatId(),
                () -> reservationCommandService.cancelReadingSeatReservation(
                        memberId,
                        reservationId));
    }

    @Override
    public void checkInReadingSeat(
            Long memberId,
            Long reservationId) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationId(reservationId);

        ReservationDto.ReadingSeatReservationDto reservation =
                reservationPolicy.requireSeatReservation(
                        reservationMapper.selectReadingSeatReservationForMember(
                                reservationId,
                                memberId),
                        "입실할 수 있는 예약을 찾을 수 없습니다.");
        reservationPolicy.validateSeatCheckIn(reservation);

        lockExecutor.withSeatLocks(
                memberId,
                reservation.getSeatId(),
                () -> reservationCommandService.checkInReadingSeat(
                        memberId,
                        reservationId));
    }

    @Override
    public ReservationDto.ReadingSeatReservationDto extendReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationId(reservationId);

        ReservationDto.ReadingSeatReservationDto reservation =
                reservationPolicy.requireSeatReservation(
                        reservationMapper.selectReadingSeatReservationForMember(
                                reservationId,
                                memberId),
                        "연장할 수 있는 예약을 찾을 수 없습니다.");

        return lockExecutor.withSeatLocks(
                memberId,
                reservation.getSeatId(),
                () -> reservationCommandService.extendReadingSeatReservation(
                        memberId,
                        reservationId));
    }

    @Override
    public List<ReservationDto.RoomAvailabilityDto> getRoomAvailability(
            Long memberId,
            LocalDate date) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationDate(date);

        LocalDateTime dateStart = date.atStartOfDay();
        LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();
        LocalDateTime serverNow =
                LocalDateTime.now(ReservationPolicy.RESERVATION_ZONE);
        List<ReservationDto.RoomReservationSlotDto> reservations =
                reservationMapper.selectRoomReservationsBetween(
                        dateStart,
                        dateEnd);

        return reservationMapper.selectActiveReservationRooms(memberId)
                .stream()
                .map(room -> roomSlotFactory.attachSlots(
                                room,
                                date,
                                serverNow,
                                reservations))
                .toList();
    }

    @Override
    public List<ReservationDto.RoomReservationDto> getMyRoomReservations(
            Long memberId) {
        reservationPolicy.requireMember(memberId);
        return reservationMapper.selectMyRoomReservations(memberId);
    }

    @Override
    public void cancelRoomReservation(
            Long memberId,
            Long reservationId) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationId(reservationId);

        ReservationDto.RoomReservationDto reservation =
                reservationPolicy.requireRoomReservation(
                        reservationMapper.selectRoomReservationForMember(
                                reservationId,
                                memberId),
                        "취소할 수 있는 공간 예약을 찾을 수 없습니다.");
        reservationPolicy.validateRoomCancellation(reservation);

        lockExecutor.withRoomLock(
                reservation.getRoomId(),
                () -> reservationCommandService.cancelRoomReservation(
                        memberId,
                        reservationId));
    }

    @Override
    public void checkInRoom(
            Long memberId,
            Long reservationId) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationId(reservationId);

        ReservationDto.RoomReservationDto reservation =
                reservationPolicy.requireRoomReservation(
                        reservationMapper.selectRoomReservationForMember(
                                reservationId,
                                memberId),
                        "입실할 수 있는 공간 예약을 찾을 수 없습니다.");
        reservationPolicy.validateRoomCheckIn(reservation);

        lockExecutor.withRoomLock(
                reservation.getRoomId(),
                () -> reservationCommandService.checkInRoom(
                        memberId,
                        reservationId));
    }

    @Override
    public void cancelAllPendingReservations(Long memberId) {
        reservationPolicy.requireMember(memberId);
        reservationMapper.cancelAllPendingReadingSeatReservations(memberId);
        reservationMapper.cancelAllPendingRoomReservations(memberId);
    }

    @Override
    public ReservationDto.RoomReservationDto reserveRoom(
            Long memberId,
            ReservationDto.RoomReservationRequestDto request) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.validateRoomReservationRequest(request);

        return lockExecutor.withRoomLock(
                request.getRoomId(),
                () -> reservationCommandService.reserveRoom(memberId, request));
    }

    private ReservationDto.ReservationDateOptionDto createDateOption(
            LocalDate today,
            int index) {
        LocalDate date = today.plusDays(index);
        int dayOfWeekValue = date.getDayOfWeek().getValue();

        return ReservationDto.ReservationDateOptionDto.builder()
                .key(date.toString())
                .date(date.toString())
                .year(date.getYear())
                .month(date.getMonthValue())
                .day(date.getDayOfMonth())
                .dayOfWeek(DAY_OF_WEEK_LABELS.get(
                        dayOfWeekValue % DAY_OF_WEEK_LABELS.size()))
                .today(index == 0)
                .sat(dayOfWeekValue == 6)
                .sun(dayOfWeekValue == 7)
                .build();
    }

    private int normalizeDateOptionDays(int days) {
        return Math.max(
                MIN_DATE_OPTION_DAYS,
                Math.min(days, MAX_DATE_OPTION_DAYS));
    }
}
