package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuProfileDto;

public interface LmsStuProfileService {
    // SLM-001 조회
    LmsStuProfileDto.ResDto requestGetLmsStudentProfile(Long memberId);
    // SLM-001 수정
    LmsStuProfileDto.ResDto requestUpdateLmsStudentProfile(Long memberId, LmsStuProfileDto.ReqDto dto);
}
