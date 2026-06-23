package com.univus.app.lms.service;

import java.util.List;

public interface LmsEnrollAsyncExecutor {

    /** 수강신청 요청을 큐에 적재. 큐가 가득 차 즉시 받을 수 없으면 false */
    boolean submit(Long memberId, List<Long> lecIds, String requestId);
}
