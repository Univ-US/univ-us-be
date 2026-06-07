package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStudentProfileResponseDto;
import com.univus.app.lms.dto.LmsStudentProfileUpdateDto;

public interface LmsStudentProfileService {
    // SLM-001 조회
    LmsStudentProfileResponseDto requestGetLmsStudentProfile(Long memberId);
    // SLM-001 수정
    LmsStudentProfileResponseDto requestUpdateLmsStudentProfile(Long memberId, LmsStudentProfileUpdateDto dto);
    // SLM-001 + SLM-012 탈퇴 요청
    void requestDeleteLmsStudentProfile(Long memberId);
}
