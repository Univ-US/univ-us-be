package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfessorProfileResponseDto;
import com.univus.app.lms.dto.LmsProfessorProfileUpdateDto;

public interface LmsProfessorProfileService {
    // PLM-001 select
    LmsProfessorProfileResponseDto requestGetLmsProfessorProfile(Long memberId);
    // PLM-001 profile update
    LmsProfessorProfileResponseDto requestUpdateLmsProfessorProfile(Long memberId, LmsProfessorProfileUpdateDto lmsProfessorProfileUpdateDto);
    // PLM-001 + PLM-012 request Withdrawal
    void requestDeleteLmsProfessorProfile(Long memberId);
}
