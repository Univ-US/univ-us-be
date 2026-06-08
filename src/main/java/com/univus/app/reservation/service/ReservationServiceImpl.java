package com.univus.app.reservation.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univus.app.reservation.dto.ReservationDto;
import com.univus.app.reservation.mapper.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final String DEFAULT_SEAT_STATUS = "RESERVED";
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<String> DAY_OF_WEEK_LABELS =
            List.of("일", "월", "화", "수", "목", "금", "토");
    private static final int MIN_DATE_OPTION_DAYS = 1;
    private static final int MAX_DATE_OPTION_DAYS = 14;
    private static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    private static final int SLOT_HOURS = 2;
    private static final int MAX_RESERVATION_HOURS = 6;

    private final ReservationMapper reservationMapper;
    private final RedissonClient redissonClient;

    @Override
    public List<ReservationDto.ReservationDateOptionDto> getReservationDateOptions(int days) {
        int dateOptionDays = normalizeDateOptionDays(days);
        LocalDate today = LocalDate.now(RESERVATION_ZONE);

        return IntStream.range(0, dateOptionDays)
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
    }

    @Override
    public List<ReservationDto.ReadingRoomAvailabilityDto> getReadingRoomAvailability(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);
        validateReservationTimePolicy(startTime, endTime);
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
        validateReservationTimePolicy(startTime, endTime);
        return reservationMapper.selectReadingSeatAvailability(readingRoomId, startTime, endTime);
    }

    @Transactional
    @Override
    public ReservationDto.ReadingSeatReservationDto reserveReadingSeat(
            Long memberId,
            ReservationDto.ReadingSeatReservationRequestDto request) {
        validateMember(memberId);
        validateReservationRequest(request);

        String lockKey = "reservation:reading-seat:" + request.getSeatId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("예약 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            int usableSeatCount = reservationMapper.countUsableReadingSeat(request.getSeatId());
            if (usableSeatCount == 0) {
                throw new IllegalArgumentException("사용 가능한 좌석이 아닙니다.");
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
            return reservation;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("예약 처리가 중단되었습니다.", ex);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<ReservationDto.ReadingSeatReservationDto> getMyReadingSeatReservations(Long memberId) {
        validateMember(memberId);
        return reservationMapper.selectMyReadingSeatReservations(memberId);
    }

    @Transactional
    @Override
    public void cancelReadingSeatReservation(Long memberId, Long reservationId) {
        validateMember(memberId);
        if (reservationId == null) {
            throw new IllegalArgumentException("예약 ID는 필수입니다.");
        }

        int updated = reservationMapper.cancelReadingSeatReservation(reservationId, memberId);
        if (updated == 0) {
            throw new IllegalArgumentException("취소할 수 있는 예약을 찾을 수 없습니다.");
        }
    }

    private void validateReservationRequest(ReservationDto.ReadingSeatReservationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("예약 요청 본문은 필수입니다.");
        }
        if (request.getSeatId() == null) {
            throw new IllegalArgumentException("좌석 ID는 필수입니다.");
        }
        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateReservationTimePolicy(request.getStartTime(), request.getEndTime());
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작 시간과 종료 시간은 필수입니다.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 뒤여야 합니다.");
        }
    }

    private void validateReservationTimePolicy(LocalDateTime startTime, LocalDateTime endTime) {
        if (!isEvenHourBoundary(startTime) || !isEvenHourBoundary(endTime)) {
            throw new IllegalArgumentException("예약 시간은 짝수 시간대의 2시간 단위로 선택해야 합니다.");
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
        long maxReservationMinutes = MAX_RESERVATION_HOURS * 60L;
        if (reservationMinutes < slotMinutes) {
            throw new IllegalArgumentException("예약은 최소 2시간부터 가능합니다.");
        }
        if (reservationMinutes > maxReservationMinutes) {
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
}
