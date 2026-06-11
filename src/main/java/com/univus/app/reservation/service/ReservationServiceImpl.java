package com.univus.app.reservation.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final String MEMBER_LOCK_KEY_PREFIX = "reservation:reading-seat:member:";
    private static final String SEAT_LOCK_KEY_PREFIX = "reservation:reading-seat:";
    private static final String ROOM_LOCK_KEY_PREFIX = "reservation:meeting-room:";
    private static final long LOCK_WAIT_SECONDS = 5L;
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<String> DAY_OF_WEEK_LABELS =
            List.of("일", "월", "화", "수", "목", "금", "토");
    private static final int MIN_DATE_OPTION_DAYS = 1;
    private static final int MAX_DATE_OPTION_DAYS = 14;
    private static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    private static final int SLOT_HOURS = 2;
    private static final int DAILY_SLOT_COUNT = (24 - 8) / SLOT_HOURS;
    private static final int MAX_RESERVATION_HOURS = 6;

    private final ReservationMapper reservationMapper;
    private final ReservationCommandService reservationCommandService;
    private final RedissonClient redissonClient;

    @Override
    public ReservationDto.ReservationDateOptionsResponseDto getReservationDateOptions(int days) {
        int dateOptionDays = normalizeDateOptionDays(days);
        LocalDateTime serverNow = LocalDateTime.now(RESERVATION_ZONE);
        LocalDate today = serverNow.toLocalDate();

        List<ReservationDto.ReservationDateOptionDto> dates = IntStream.range(0, dateOptionDays)
                .mapToObj(index -> {
                    LocalDate date = today.plusDays(index);
                    int dayOfWeekValue = date.getDayOfWeek().getValue();
                    int dayLabelIndex = dayOfWeekValue % DAY_OF_WEEK_LABELS.size();

                    return ReservationDto.ReservationDateOptionDto.builder()
                            .key(date.toString())
                            .date(date.toString())
                            .year(date.getYear())
                            .month(date.getMonthValue())
                            .day(date.getDayOfMonth())
                            .dayOfWeek(DAY_OF_WEEK_LABELS.get(dayLabelIndex))
                            .today(index == 0)
                            .sat(dayOfWeekValue == 6)
                            .sun(dayOfWeekValue == 7)
                            .build();
                })
                .toList();

        return ReservationDto.ReservationDateOptionsResponseDto.builder()
                .serverNow(serverNow)
                .dates(dates)
                .build();
    }

    @Override
    public List<ReservationDto.ReadingRoomAvailabilityDto> getReadingRoomAvailability(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        validateReservationTimePolicy(startTime, endTime, MAX_RESERVATION_HOURS);
        return reservationMapper.selectReadingRoomAvailability(startTime, endTime);
    }

    @Override
    public List<ReservationDto.ReadingSeatAvailabilityDto> getReadingSeatAvailability(
            Long readingRoomId,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (readingRoomId == null) {
            throw new IllegalArgumentException("독서실 ID는 필수입니다.");
        }
        validateTimeRange(startTime, endTime);
        validateReservationTimePolicy(startTime, endTime, MAX_RESERVATION_HOURS);
        return reservationMapper.selectReadingSeatAvailability(readingRoomId, startTime, endTime);
    }

    @Override
    public ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request) {
        validateMember(memberId);
        validateReservationRequest(request);

        return executeWithLocks(
                List.of(
                        redissonClient.getLock(MEMBER_LOCK_KEY_PREFIX + memberId),
                        redissonClient.getLock(SEAT_LOCK_KEY_PREFIX + request.getSeatId())),
                () -> reservationCommandService.reserveReadingSeat(memberId, request));
    }

    @Override
    public List<ReservationDto.ReadingSeatReservationDto> getMyReadingSeatReservations(Long memberId) {
        validateMember(memberId);
        return reservationMapper.selectMyReadingSeatReservations(memberId);
    }

    @Override
    public void cancelReadingSeatReservation(Long memberId, Long reservationId) {
        validateMember(memberId);
        if (reservationId == null) {
            throw new IllegalArgumentException("예약 ID는 필수입니다.");
        }

        ReservationDto.ReadingSeatReservationDto reservation =
                reservationMapper.selectReadingSeatReservationForMember(reservationId, memberId);
        if (reservation == null) {
            throw new IllegalArgumentException("취소할 수 있는 예약을 찾을 수 없습니다.");
        }
        if (!isCancelableStatus(reservation.getStatus())) {
            throw new IllegalArgumentException("이미 취소되었거나 완료된 예약입니다.");
        }

        executeWithLocks(
                List.of(
                        redissonClient.getLock(MEMBER_LOCK_KEY_PREFIX + memberId),
                        redissonClient.getLock(SEAT_LOCK_KEY_PREFIX + reservation.getSeatId())),
                () -> {
                    reservationCommandService.cancelReadingSeatReservation(memberId, reservationId);
                    return null;
                });
    }

    @Override
    public void checkInReadingSeat(Long memberId, Long reservationId) {
        validateMember(memberId);
        if (reservationId == null) {
            throw new IllegalArgumentException("예약 ID는 필수입니다.");
        }
        
        ReservationDto.ReadingSeatReservationDto reservation =
                reservationMapper.selectReadingSeatReservationForMember(reservationId, memberId);
        if (reservation == null) {
            throw new IllegalArgumentException("입실할 수 있는 예약을 찾을 수 없습니다.");
        }

        executeWithLocks(
                List.of(
                        redissonClient.getLock(MEMBER_LOCK_KEY_PREFIX + memberId),
                        redissonClient.getLock(SEAT_LOCK_KEY_PREFIX + reservation.getSeatId())),
                () -> {
                    reservationCommandService.checkInReadingSeat(memberId, reservationId);
                    return null;
                });
    }

    @Override
    public ReservationDto.ReadingSeatReservationDto extendReadingSeatReservation(Long memberId, Long reservationId) {
        validateMember(memberId);
        if (reservationId == null) {
            throw new IllegalArgumentException("예약 ID는 필수입니다.");
        }
        
        ReservationDto.ReadingSeatReservationDto reservation =
                reservationMapper.selectReadingSeatReservationForMember(reservationId, memberId);
        if (reservation == null) {
            throw new IllegalArgumentException("연장할 수 있는 예약을 찾을 수 없습니다.");
        }

        return executeWithLocks(
                List.of(
                        redissonClient.getLock(MEMBER_LOCK_KEY_PREFIX + memberId),
                        redissonClient.getLock(SEAT_LOCK_KEY_PREFIX + reservation.getSeatId())),
                () -> reservationCommandService.extendReadingSeatReservation(memberId, reservationId));
    }

    @Override
    public List<ReservationDto.RoomAvailabilityDto> getRoomAvailability(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("예약 날짜는 필수입니다.");
        }

        LocalDateTime dateStart = date.atStartOfDay();
        LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();
        LocalDateTime serverNow = LocalDateTime.now(RESERVATION_ZONE);
        List<ReservationDto.RoomReservationSlotDto> reservations =
                reservationMapper.selectRoomReservationsBetween(dateStart, dateEnd);

        return reservationMapper.selectActiveReservationRooms().stream()
                .map(room -> {
                    room.setSlots(buildRoomSlots(room.getRoomId(), date, serverNow, reservations));
                    return room;
                })
                .toList();
    }

    @Override
    public List<ReservationDto.RoomReservationDto> getMyRoomReservations(Long memberId) {
        validateMember(memberId);
        return reservationMapper.selectMyRoomReservations(memberId);
    }

    @Override
    public void cancelRoomReservation(Long memberId, Long reservationId) {
        validateMember(memberId);
        if (reservationId == null) {
            throw new IllegalArgumentException("예약 ID는 필수입니다.");
        }

        ReservationDto.RoomReservationDto reservation =
                reservationMapper.selectRoomReservationForMember(reservationId, memberId);
        if (reservation == null) {
            throw new IllegalArgumentException("취소할 수 있는 공간 예약을 찾을 수 없습니다.");
        }
        if (!isCancelableStatus(reservation.getStatus())) {
            throw new IllegalArgumentException("이미 취소되었거나 완료된 예약입니다.");
        }

        executeWithLocks(
                List.of(redissonClient.getLock(ROOM_LOCK_KEY_PREFIX + reservation.getRoomId())),
                () -> {
                    reservationCommandService.cancelRoomReservation(memberId, reservationId);
                    return null;
                });
    }

    @Override
    public ReservationDto.RoomReservationDto reserveRoom(
            Long memberId,
            ReservationDto.RoomReservationRequestDto request) {
        validateMember(memberId);
        validateRoomReservationRequest(request);

        return executeWithLocks(
                List.of(redissonClient.getLock(ROOM_LOCK_KEY_PREFIX + request.getRoomId())),
                () -> reservationCommandService.reserveRoom(memberId, request));
    }

    private <T> T executeWithLocks(List<RLock> locks, LockedOperation<T> operation) {
        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            for (RLock lock : locks) {
                boolean locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
                if (!locked) {
                    throw new IllegalStateException("예약 처리 중입니다. 잠시 후 다시 시도해주세요.");
                }
                acquiredLocks.add(lock);
            }

            return operation.execute();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("예약 처리가 중단되었습니다.", ex);
        } finally {
            for (int index = acquiredLocks.size() - 1; index >= 0; index--) {
                unlockIfHeldByCurrentThread(acquiredLocks.get(index));
            }
        }
    }

    private void unlockIfHeldByCurrentThread(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private List<ReservationDto.RoomReservationSlotDto> buildRoomSlots(
            Long roomId,
            LocalDate date,
            LocalDateTime serverNow,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        return IntStream.range(0, DAILY_SLOT_COUNT)
                .mapToObj(index -> {
                    LocalDateTime slotStart = date.atTime(OPEN_TIME.plusHours((long) index * SLOT_HOURS));
                    LocalDateTime slotEnd = slotStart.plusHours(SLOT_HOURS);
                    ReservationDto.RoomReservationSlotDto overlappingReservation =
                            findOverlappingRoomReservation(roomId, slotStart, slotEnd, reservations);

                    return ReservationDto.RoomReservationSlotDto.builder()
                            .roomId(roomId)
                            .reservationId(overlappingReservation == null
                                    ? null
                                    : overlappingReservation.getReservationId())
                            .reservedMemberId(overlappingReservation == null
                                    ? null
                                    : overlappingReservation.getReservedMemberId())
                            .startTime(slotStart)
                            .endTime(slotEnd)
                            .status(overlappingReservation == null
                                    ? null
                                    : overlappingReservation.getStatus())
                            .available(overlappingReservation == null && slotEnd.isAfter(serverNow))
                            .build();
                })
                .toList();
    }

    private ReservationDto.RoomReservationSlotDto findOverlappingRoomReservation(
            Long roomId,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            List<ReservationDto.RoomReservationSlotDto> reservations) {
        return reservations.stream()
                .filter(reservation -> roomId.equals(reservation.getRoomId())
                        && reservation.getStartTime().isBefore(slotEnd)
                        && reservation.getEndTime().isAfter(slotStart))
                .findFirst()
                .orElse(null);
    }

    private void validateRoomReservationRequest(ReservationDto.RoomReservationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("예약 요청 본문은 필수입니다.");
        }
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("공간 ID는 필수입니다.");
        }
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateReservationTimePolicy(request.getStartTime(), request.getEndTime(), null);
    }

    private void validateReservationRequest(ReservationDto.ReadingSeatReservationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("예약 요청 본문은 필수입니다.");
        }
        if (request.getSeatId() == null) {
            throw new IllegalArgumentException("좌석 ID는 필수입니다.");
        }
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateReservationTimePolicy(request.getStartTime(), request.getEndTime(), MAX_RESERVATION_HOURS);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작 시간과 종료 시간은 필수입니다.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 뒤여야 합니다.");
        }
    }

    private void validateReservationTimePolicy(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer maxReservationHours) {
        if (!isEvenHourBoundary(startTime) || !isEvenHourBoundary(endTime)) {
            throw new IllegalArgumentException("예약 시간은 짝수 시간대의 2시간 단위로 선택해야 합니다.");
        }

        if (!endTime.isAfter(LocalDateTime.now(RESERVATION_ZONE))) {
            throw new IllegalArgumentException("이미 종료된 예약 시간입니다.");
        }

        LocalDateTime closeTime = startTime.toLocalDate().plusDays(1).atStartOfDay();
        if (startTime.toLocalTime().isBefore(OPEN_TIME) || endTime.isAfter(closeTime)) {
            throw new IllegalArgumentException("예약 가능 시간은 08:00부터 24:00까지입니다.");
        }

        if (!endTime.toLocalDate().equals(startTime.toLocalDate()) && !endTime.equals(closeTime)) {
            throw new IllegalArgumentException("예약은 하루 운영 시간 안에서만 가능합니다.");
        }

        long reservationMinutes = Duration.between(startTime, endTime).toMinutes();
        long slotMinutes = SLOT_HOURS * 60L;
        if (reservationMinutes < slotMinutes) {
            throw new IllegalArgumentException("예약은 최소 2시간부터 가능합니다.");
        }
        if (maxReservationHours != null
                && reservationMinutes > maxReservationHours * 60L) {
            throw new IllegalArgumentException("예약은 최대 6시간까지 가능합니다.");
        }
        if (reservationMinutes % slotMinutes != 0) {
            throw new IllegalArgumentException("예약 시간은 2시간 단위여야 합니다.");
        }
    }

    private boolean isEvenHourBoundary(LocalDateTime dateTime) {
        return dateTime.getMinute() == 0
                && dateTime.getSecond() == 0
                && dateTime.getNano() == 0
                && dateTime.getHour() % SLOT_HOURS == 0;
    }

    private int normalizeDateOptionDays(int days) {
        if (days < MIN_DATE_OPTION_DAYS) {
            return MIN_DATE_OPTION_DAYS;
        }
        return Math.min(days, MAX_DATE_OPTION_DAYS);
    }

    private void validateMember(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }

    private boolean isCancelableStatus(String status) {
        return "RESERVED".equals(status) || "USING".equals(status);
    }

    @FunctionalInterface
    private interface LockedOperation<T> {
        T execute();
    }
}
