package com.univus.app.commoncode.service;

import com.univus.app.commoncode.dto.CommonCodeResponseDto;

import java.util.List;

public interface CommonCodeService {

    /* 그룹코드(GRP_CODE)별 공통코드 목록 (FE 드롭다운) */
    List<CommonCodeResponseDto> getCodesByGroupCode(String groupCode);
}
