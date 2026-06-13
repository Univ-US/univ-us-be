package com.univus.app.lms.code;

import com.univus.app.commoncode.code.CommonCode;

/* LMS 탈퇴 요청 상태 코드 — LMS_USER_SECESSION_REQUEST.STATUS (공통코드 그룹 LMS_USR_SEC_REQ_STATUS)
 * 라벨/순서는 DB COMMON_CODE(2026-06-13)와 동기화. */
public enum LmsUsrSecReqStatusCode implements CommonCode {
    REQUESTED("REQ", "요청", 1),
    APPROVED("APR", "승인", 2),
    REJECTED("REJ", "반려", 3),
    CANCELED("CAN", "요청취소", 4);

    private final String code;
    private final String label;
    private final int order;

    LmsUsrSecReqStatusCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @Override public String getCode()  { return code; }

    @Override public String getLabel() { return label; }

    @Override public int getOrder()    { return order; }

    public static LmsUsrSecReqStatusCode fromCode(String code) {
        for (LmsUsrSecReqStatusCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("알 수 없는 탈퇴 요청 상태 코드: " + code);
    }
}
