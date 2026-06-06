package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 날짜코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DayCode implements CommonCode {

    MON("MON", "월요일", 1),
    TUE("TUE", "화요일", 2),
    WED("WED", "수요일", 3),
    THU("THU", "목요일", 4),
    FRI("FRI", "금요일", 5),
    SAT("SAT", "토요일", 6),
    SUN("SUN", "일요일", 7);

    private final String code;
    private final String label;
    private final int order;

    DayCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @Override
    @JsonProperty("code")
    public String getCode()   { return code; }

    @Override
    @JsonProperty("label")
    public String getLabel()  { return label; }

    @Override
    @JsonProperty("order")
    public int getOrder() {
        return order;
    }

    public static DayCode fromCode(String code) {
        for (DayCode d : values()) {
            if (d.code.equals(code)) return d;
        }
        throw new IllegalArgumentException("알 수 없는 날짜코드: " + code);
    }
}