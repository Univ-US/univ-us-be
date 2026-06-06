package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 출석 상태코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AttendanceStatusCode implements CommonCode {

    PRS("PRS", "출석", 1),
    LAT("LAT", "지각", 2),
    ABS("ABS", "결석", 3),
    ELV("ELV", "조퇴", 4),
    EXC("EXC", "공결", 5);

    private final String code;
    private final String label;
    private final int order;

    AttendanceStatusCode(String code, String label, int order) {
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

    public static AttendanceStatusCode fromCode(String code) {
        for (AttendanceStatusCode a : values()) {
            if (a.code.equals(code)) return a;
        }
        throw new IllegalArgumentException("알 수 없는 출석 상태코드: " + code);
    }

}
