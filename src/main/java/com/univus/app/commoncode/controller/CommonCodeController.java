package com.univus.app.commoncode.controller;

import com.univus.app.commoncode.dto.CommonCodeResponseDto;
import com.univus.app.commoncode.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 공통코드 조회 (FE 드롭다운용) — permitAll */
@RestController
@RequestMapping("/api/common-codes")
@RequiredArgsConstructor
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    /** 그룹코드별 공통코드 목록 — 예: GET /api/common-codes/SEM_TERM */
    @GetMapping("/{groupCode}")
    public ResponseEntity<List<CommonCodeResponseDto>> getCodesByGroupCode(@PathVariable String groupCode) {
        return ResponseEntity.ok(commonCodeService.getCodesByGroupCode(groupCode));
    }
}
