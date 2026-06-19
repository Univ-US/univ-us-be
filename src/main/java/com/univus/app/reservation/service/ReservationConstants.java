package com.univus.app.reservation.service;

import java.time.LocalTime;
import java.time.ZoneId;

public final class ReservationConstants {

    public static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    public static final int SLOT_HOURS = 2;
    public static final int CHECK_IN_WINDOW_MINUTES = 20;
    public static final int MAX_SEAT_RESERVATION_HOURS = 6;
    public static final int MAX_SEAT_USAGE_HOURS = 10;
    public static final int EXTENSION_HOURS = 2;
    public static final int EXTENSION_WINDOW_MINUTES = 20;
    public static final int PENALTY_BLOCK_THRESHOLD = 5;
    public static final String PENALTY_PLEDGE_PHRASE =
            "예약한 시설은 책임 있게 이용하며, 사전 취소 없이 이용하지 않는 일이 반복되지 않도록 유의하겠습니다.";
    public static final String PENALTY_BLOCK_MESSAGE =
            "노쇼 패널티가 5회 누적되어 예약이 제한되었습니다. 서약서를 확인하면 다시 예약할 수 있습니다.";
    public static final String PENALTY_AVAILABLE_MESSAGE = "예약 이용이 가능합니다.";

    private ReservationConstants() {
    }
}
