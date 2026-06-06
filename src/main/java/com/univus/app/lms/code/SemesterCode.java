package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 학기 코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SemesterCode implements CommonCode {
    SM1("SM1", "1학기",       1),
    SM2("SM2", "2학기",       2),
    SMR("SMR", "여름계절학기", 3),
    WNT("WNT", "겨울계절학기", 4);

    private final String code;
    private final String label;
    private final int order;

    SemesterCode(String code, String label, int order) {
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

    public static SemesterCode fromCode(String code) {
        for (SemesterCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("알 수 없는 학기 코드: " + code);
    }

}
