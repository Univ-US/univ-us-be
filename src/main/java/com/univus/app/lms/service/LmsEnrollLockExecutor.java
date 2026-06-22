package com.univus.app.lms.service;

import java.util.List;
import java.util.function.Supplier;

public interface LmsEnrollLockExecutor {

    /** lecIds에 해당하는 강좌들을 잠근 뒤 operation을 실행. 잠금 획득 실패 시 ConflictException */
    <T> T withLectureLocks(List<Long> lecIds, Supplier<T> operation);
}
