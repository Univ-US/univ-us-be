package com.univus.app.lms.code;

import com.univus.app.commoncode.code.CommonCode;

/* 첨부 유효 상태 코드 — LMS 첨부 공통 패턴의 *_ATT_VAL_STATUS (공통코드 그룹 ATT_VAL_STATUS)
 * 라벨/순서는 DB COMMON_CODE와 동기화. */
public enum AttValStatusCode implements CommonCode {
    ACTIVE("ACT", "정상", 1),
    DELETED("DEL", "삭제", 2),
    EXPIRED("EXPR", "만료", 3),
    FAILED("FAIL", "업로드실패", 4);

    private final String code;
    private final String label;
    private final int order;

    AttValStatusCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @Override public String getCode()  { return code; }

    @Override public String getLabel() { return label; }

    @Override public int getOrder()    { return order; }

    public static AttValStatusCode fromCode(String code) {
        for (AttValStatusCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("알 수 없는 첨부 유효 상태 코드: " + code);
    }
}
