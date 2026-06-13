package com.univus.app.lms.code;

import com.univus.app.commoncode.code.CommonCode;

/* 과제 제출 상태 코드 — LECTURE_ASSIGNMENT_SUBMISSION.LEC_ASN_SBM_STATUS (공통코드 그룹 LEC_ASN_SBM_STATUS)
 * 라벨/순서는 DB COMMON_CODE(2026-06-13)와 동기화. MyBatis XML 은 enum 을 못 쓰므로 SQL 리터럴('NSB' 등)은 유지. */
public enum LecAsnSbmStatusCode implements CommonCode {
    NOT_SUBMITTED("NSB", "미제출", 1),
    SUBMITTED("SBM", "제출", 2),
    GRADED("GRD", "채점완료", 3),
    RETURNED("RTN", "반려", 4);

    private final String code;
    private final String label;
    private final int order;

    LecAsnSbmStatusCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @Override public String getCode()  { return code; }

    @Override public String getLabel() { return label; }

    @Override public int getOrder()    { return order; }

    public static LecAsnSbmStatusCode fromCode(String code) {
        for (LecAsnSbmStatusCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("알 수 없는 과제 제출 상태 코드: " + code);
    }
}
