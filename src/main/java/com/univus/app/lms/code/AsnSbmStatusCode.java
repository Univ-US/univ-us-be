package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 출석 상태코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AsnSbmStatusCode implements CommonCode {

    NSB("NSB", "미제출",   1),
    SBM("SBM", "제출",     2),
    GRD("GRD", "채점완료", 3),
    RTN("RTN", "반려",     4);

    private final String code;
    private final String label;
    private final int order;

    AsnSbmStatusCode(String code, String label, int order) {
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

    public static AsnSbmStatusCode fromCode(String code) {
        for (AsnSbmStatusCode a : values()) {
            if (a.code.equals(code)) return a;
        }
        throw new IllegalArgumentException("알 수 없는 출석 상태코드: " + code);
    }

}
