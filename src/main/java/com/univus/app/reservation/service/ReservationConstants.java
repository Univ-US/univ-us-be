package com.univus.app.reservation.service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public final class ReservationConstants {

    // 예약 및 채팅 상태
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_USING = "USING";
    public static final String CHAT_ROOM_STATUS_ACTIVE = "ACTIVE";

    // 실시간 예약 이벤트
    public static final String ACTION_RESERVED = STATUS_RESERVED;
    public static final String ACTION_CANCELLED = "CANCELLED";
    public static final String ACTION_CHECKED_IN = "CHECKED_IN";
    public static final String ACTION_EXTENDED = "EXTENDED";
    public static final String ACTION_COMPLETED = "COMPLETED";

    // 예약 시간 정책
    public static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    public static final int SLOT_HOURS = 2;
    public static final int DAILY_ROOM_SLOT_COUNT =
            (24 - OPEN_TIME.getHour()) / SLOT_HOURS;
    public static final int CHECK_IN_WINDOW_MINUTES = 20;
    public static final int MAX_SEAT_RESERVATION_HOURS = 6;
    public static final int MAX_SEAT_USAGE_HOURS = 10;
    public static final int EXTENSION_HOURS = 2;
    public static final int EXTENSION_WINDOW_MINUTES = 20;
    public static final int MIN_DATE_OPTION_DAYS = 1;
    public static final int MAX_DATE_OPTION_DAYS = 14;
    public static final List<String> DAY_OF_WEEK_LABELS =
            List.of("일", "월", "화", "수", "목", "금", "토");

    // 패널티 정책
    public static final int PENALTY_BLOCK_THRESHOLD = 5;
    public static final String PENALTY_TYPE_NO_SHOW = "NO_SHOW";
    public static final String PENALTY_PLEDGE_PHRASE =
            "예약한 시설은 책임 있게 이용하며, 사전 취소 없이 이용하지 않는 일이 반복되지 않도록 유의하겠습니다.";
    public static final String PENALTY_BLOCK_MESSAGE =
            "노쇼 패널티가 5회 누적되어 예약이 제한되었습니다. 서약서를 확인하면 다시 예약할 수 있습니다.";
    public static final String PENALTY_AVAILABLE_MESSAGE = "예약 이용이 가능합니다.";
    public static final String SEAT_NO_SHOW_REASON =
            "독서실 좌석 입실 가능 시간 내 입실하지 않아 자동 취소되었습니다.";
    public static final String ROOM_NO_SHOW_REASON =
            "회의실 예약 시작 후 20분 이내 입실하지 않아 자동 취소되었습니다.";

    // Redis Lock
    public static final String MEMBER_LOCK_KEY_PREFIX =
            "reservation:reading-seat:member:";
    public static final String SEAT_LOCK_KEY_PREFIX =
            "reservation:reading-seat:";
    public static final String ROOM_LOCK_KEY_PREFIX =
            "reservation:meeting-room:";
    public static final long LOCK_WAIT_SECONDS = 5L;
    public static final String SCHEDULER_LOCK_KEY =
            "reservation:scheduler:lifecycle";
    public static final int SCHEDULER_BATCH_SIZE = 100;

    // WebSocket 경로
    public static final String SEAT_REALTIME_TOPIC =
            "/sub/reservations/seats";
    public static final String ROOM_REALTIME_TOPIC =
            "/sub/reservations/rooms";
    public static final String USER_SEAT_REALTIME_QUEUE =
            "/queue/reservations/seats";
    public static final String USER_ROOM_REALTIME_QUEUE =
            "/queue/reservations/rooms";
    public static final String SEAT_CHAT_USER_QUEUE_PREFIX =
            "/queue/seat-chats/";

    // 좌석 채팅 정책
    public static final int MAX_SEAT_CHAT_MESSAGE_LENGTH = 2000;

    private ReservationConstants() {
    }
}
