package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 과제제출 가능여부 코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AsnSbmAvailCode implements CommonCode {
    AVL("AVL", "제출가능", 1),
    MOD("MOD", "수정가능", 2),
    LAT("LAT", "지각제출", 3),
    CLS("CLS", "마감",     4),
    NOP("NOP", "제출불가", 5);

    private final String code;
    private final String label;
    private final int order;

    AsnSbmAvailCode(String code, String label, int order) {
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

    public static AsnSbmAvailCode fromCode(String code) {
        for (AsnSbmAvailCode a : values()) {
            if (a.code.equals(code)) return a;
        }
        throw new IllegalArgumentException("알 수 없는 과제제출 가능여부 코드: " + code);
    }
}
