package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 회원탈퇴 요청상태 코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SecReqStatusCode implements CommonCode {
    REQ("REQ", "요청",     1),
    APR("APR", "승인",     2),
    REJ("REJ", "반려",     3),
    CAN("CAN", "요청취소", 4);

    private final String code;
    private final String label;
    private final int order;

    SecReqStatusCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @JsonProperty("code")
    @Override public String getCode()  { return code; }

    @JsonProperty("label")
    @Override public String getLabel() { return label; }

    @JsonProperty("order")
    @Override public int getOrder()    { return order; }

    public static SecReqStatusCode fromCode(String code) {
        for (SecReqStatusCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("알 수 없는 회원탈퇴 요청상태 코드: " + code);
    }

}
