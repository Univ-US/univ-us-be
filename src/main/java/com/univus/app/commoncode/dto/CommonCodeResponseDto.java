package com.univus.app.commoncode.dto;

import lombok.*;

/** 공통코드 1건 (FE 드롭다운 항목) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonCodeResponseDto {
    private String codeVal;    // COMMON_CODE.CODE_VAL (예: SEM1)
    private String codeName;   // COMMON_CODE.CODE_NM  (예: 1학기)
    private Integer codeOrder; // COMMON_CODE.CODE_ORDER (정렬 순서)
}
