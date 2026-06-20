package com.univus.app.lms.code;

import com.univus.app.commoncode.code.CommonCode;

/* 과제 유효 상태 코드 — LECTURE_ASSIGNMENT.LEC_ASN_VAL_STATUS (공통코드 그룹 LEC_ASN_VAL_STATUS)
 * 라벨/순서는 DB COMMON_CODE와 동기화. 등록 시 'AVL' 고정. */
public enum LecAsnValStatusCode implements CommonCode {
    AVAILABLE("AVL", "제출가능", 1),
    MODIFIABLE("MOD", "수정가능", 2),
    LATE("LAT", "지각제출", 3),
    CLOSED("CLS", "마감", 4),
    NOT_ALLOWED("NOP", "제출불가", 5);

    private final String code;
    private final String label;
    private final int order;

    LecAsnValStatusCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @Override public String getCode()  { return code; }

    @Override public String getLabel() { return label; }

    @Override public int getOrder()    { return order; }

    public static LecAsnValStatusCode fromCode(String code) {
        for (LecAsnValStatusCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("알 수 없는 과제 유효 상태 코드: " + code);
    }
}
