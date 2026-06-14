package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfProfileDto;

public interface LmsProfProfileService {
    // PLM-001 select
    LmsProfProfileDto.ResDto requestGetLmsProfessorProfile(Long memberId);
    // PLM-001 profile update
    LmsProfProfileDto.ResDto requestUpdateLmsProfessorProfile(Long memberId, LmsProfProfileDto.ReqDto lmsProfessorProfileUpdateDto);
    // PLM-001 + PLM-012 request Withdrawal
    void requestDeleteLmsProfessorProfile(Long memberId);
}
