package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 수강 상태코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EnrStatusCode implements CommonCode {

    ENR("ENR", "수강중",   1),
    CMP("CMP", "수료",     2),
    DRP("DRP", "수강취소", 3),
    FAL("FAL", "미수료",   4);

    private final String code;
    private final String label;
    private final int order;

    EnrStatusCode(String code, String label, int order) {
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

    public static EnrStatusCode fromCode(String code) {
        for (EnrStatusCode e : values()) {
            if (e.code.equals(code)) return e;
        }
        throw new IllegalArgumentException("알 수 없는 수강 상태코드: " + code);
    }

}
