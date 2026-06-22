package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuEnrollDto;

import java.util.List;

public interface LmsStuEnrollService {

    /** 신청 가능 학점 한도 등 상단 요약 */
    LmsStuEnrollDto.SummaryResDto getSummary(Long memberId);

    /** 개설 강좌 전체 목록 (검색/필터는 화면에서 처리) */
    List<LmsStuEnrollDto.LectureRow> getOpenLectures(Long memberId);

    /** 장바구니 일괄 신청 (정원/시간표충돌/학점한도는 서버가 최종 검증) */
    LmsStuEnrollDto.SubmitResultDto submitEnrollment(Long memberId, List<Long> lecIds);

    /** 신청 취소 */
    void cancelEnrollment(Long memberId, Long lecId);
}
