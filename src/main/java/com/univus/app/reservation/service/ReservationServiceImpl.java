package com.univus.app.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.common.PaginateUtilRestApi;
import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.reservation.dto.ReadingRoomAvailabilityDto;
import com.univus.app.reservation.dto.ReadingSeatAvailabilityDto;
import com.univus.app.reservation.dto.ReadingSeatReservationDto;
import com.univus.app.reservation.dto.ReadingSeatReservationRequestDto;
import com.univus.app.reservation.dto.ReservationDateOptionDto;
import com.univus.app.reservation.dto.ReservationDateOptionsResponseDto;
import com.univus.app.reservation.dto.ReservationPenaltyHistoryDto;
import com.univus.app.reservation.dto.ReservationPenaltyPledgeRequestDto;
import com.univus.app.reservation.dto.ReservationPenaltyStatusDto;
import com.univus.app.reservation.dto.RoomAvailabilityDto;
import com.univus.app.reservation.dto.RoomReservationDto;
import com.univus.app.reservation.dto.RoomReservationRequestDto;
import com.univus.app.reservation.dto.RoomReservationSlotDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl
        implements ReservationService, PendingReservationCancellationService {

    private final ReservationMapper reservationMapper;
    private final ReservationCommandService reservationCommandService;
    private final ReservationPolicy reservationPolicy;
    private final ReservationLockExecutor lockExecutor;
    private final RoomSlotFactory roomSlotFactory;

    @Override
    public ReservationDateOptionsResponseDto getReservationDateOptions(
            int days) {
        LocalDateTime serverNow =
                LocalDateTime.now(ReservationConstants.RESERVATION_ZONE);
        LocalDate today = serverNow.toLocalDate();
        int dateOptionDays = normalizeDateOptionDays(days);
        List<ReservationDateOptionDto> dates =
                new ArrayList<>();
        for (int index = 0; index < dateOptionDays; index++) {
            dates.add(createDateOption(today, index));
        }

        return ReservationDateOptionsResponseDto.builder()
                .serverNow(serverNow)
                .dates(dates)
                .build();
    }

    @Override
    public ReservationPenaltyStatusDto getReservationPenaltyStatus(
            Long memberId) {
        reservationPolicy.requireMember(memberId);
        return reservationPolicy.buildPenaltyStatus(
                reservationMapper.countActiveReservationPenalties(memberId));
    }

    @Override
    public PaginateUtilRestApiRes<ReservationPenaltyHistoryDto>
            getReservationPenaltyHistory(
                    Long memberId,
                    Integer page,
                    Integer size) {
        reservationPolicy.requireMember(memberId);
        int safePage = PaginateUtilRestApi.normalizePage(page);
        int safeSize = PaginateUtilRestApi.normalizeSize(size);
        List<ReservationPenaltyHistoryDto> history =
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
    public ReservationPenaltyStatusDto pledgeReservationPenalty(
            Long memberId,
            ReservationPenaltyPledgeRequestDto request) {
        reservationPolicy.requireMember(memberId);
        int activePenaltyCount =
                reservationMapper.countActiveReservationPenalties(memberId);
        reservationPolicy.validatePenaltyPledge(request, activePenaltyCount);

        reservationMapper.pledgeActiveReservationPenalties(memberId);
        return reservationPolicy.buildPenaltyStatus(
                reservationMapper.countActiveReservationPenalties(memberId));
    }

    @Override
    public List<ReadingRoomAvailabilityDto>
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
    public List<ReadingSeatAvailabilityDto>
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
    public ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReadingSeatReservationRequestDto request) {
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
    public List<ReadingSeatReservationDto>
            getMyReadingSeatReservations(Long memberId) {
        reservationPolicy.requireMember(memberId);
        return reservationMapper.selectMyReadingSeatReservations(memberId);
    }

    @Override
    public void cancelReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        ReadingSeatReservationDto reservation =
                getRequiredSeatReservation(
                        memberId,
                        reservationId,
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
        ReadingSeatReservationDto reservation =
                getRequiredSeatReservation(
                        memberId,
                        reservationId,
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
    public ReadingSeatReservationDto extendReadingSeatReservation(
            Long memberId,
            Long reservationId) {
        ReadingSeatReservationDto reservation =
                getRequiredSeatReservation(
                        memberId,
                        reservationId,
                        "연장할 수 있는 예약을 찾을 수 없습니다.");

        return lockExecutor.withSeatLocks(
                memberId,
                reservation.getSeatId(),
                () -> reservationCommandService.extendReadingSeatReservation(
                        memberId,
                        reservationId));
    }

    @Override
    public List<RoomAvailabilityDto> getRoomAvailability(
            Long memberId,
            LocalDate date) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationDate(date);

        LocalDateTime dateStart = date.atStartOfDay();
        LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();
        LocalDateTime serverNow =
                LocalDateTime.now(ReservationConstants.RESERVATION_ZONE);
        List<RoomReservationSlotDto> reservations =
                reservationMapper.selectRoomReservationsBetween(
                        dateStart,
                        dateEnd);
        List<RoomAvailabilityDto> rooms =
                reservationMapper.selectActiveReservationRooms(memberId);
        List<RoomAvailabilityDto> roomsWithSlots =
                new ArrayList<>();

        for (RoomAvailabilityDto room : rooms) {
            RoomAvailabilityDto roomWithSlots =
                    roomSlotFactory.attachSlots(
                            room,
                            date,
                            serverNow,
                            reservations);
            roomsWithSlots.add(roomWithSlots);
        }
        return roomsWithSlots;
    }

    @Override
    public List<RoomReservationDto> getMyRoomReservations(
            Long memberId) {
        reservationPolicy.requireMember(memberId);
        return reservationMapper.selectMyRoomReservations(memberId);
    }

    @Override
    public void cancelRoomReservation(
            Long memberId,
            Long reservationId) {
        RoomReservationDto reservation =
                getRequiredRoomReservation(
                        memberId,
                        reservationId,
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
        RoomReservationDto reservation =
                getRequiredRoomReservation(
                        memberId,
                        reservationId,
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
    public RoomReservationDto reserveRoom(
            Long memberId,
            RoomReservationRequestDto request) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.validateRoomReservationRequest(request);

        return lockExecutor.withRoomLock(
                request.getRoomId(),
                () -> reservationCommandService.reserveRoom(memberId, request));
    }

    private ReadingSeatReservationDto getRequiredSeatReservation(
            Long memberId,
            Long reservationId,
            String notFoundMessage) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationId(reservationId);

        ReadingSeatReservationDto reservation =
                reservationMapper.selectReadingSeatReservationForMember(
                        reservationId,
                        memberId);
        return reservationPolicy.requireSeatReservation(
                reservation,
                notFoundMessage);
    }

    private RoomReservationDto getRequiredRoomReservation(
            Long memberId,
            Long reservationId,
            String notFoundMessage) {
        reservationPolicy.requireMember(memberId);
        reservationPolicy.requireReservationId(reservationId);

        RoomReservationDto reservation =
                reservationMapper.selectRoomReservationForMember(
                        reservationId,
                        memberId);
        return reservationPolicy.requireRoomReservation(
                reservation,
                notFoundMessage);
    }

    private ReservationDateOptionDto createDateOption(
            LocalDate today,
            int index) {
        LocalDate date = today.plusDays(index);
        int dayOfWeekValue = date.getDayOfWeek().getValue();

        return ReservationDateOptionDto.builder()
                .key(date.toString())
                .date(date.toString())
                .year(date.getYear())
                .month(date.getMonthValue())
                .day(date.getDayOfMonth())
                .dayOfWeek(ReservationConstants.DAY_OF_WEEK_LABELS.get(
                        dayOfWeekValue
                                % ReservationConstants.DAY_OF_WEEK_LABELS.size()))
                .today(index == 0)
                .sat(dayOfWeekValue == 6)
                .sun(dayOfWeekValue == 7)
                .build();
    }

    private int normalizeDateOptionDays(int days) {
        if (days < ReservationConstants.MIN_DATE_OPTION_DAYS) {
            return ReservationConstants.MIN_DATE_OPTION_DAYS;
        }
        if (days > ReservationConstants.MAX_DATE_OPTION_DAYS) {
            return ReservationConstants.MAX_DATE_OPTION_DAYS;
        }
        return days;
    }
}
