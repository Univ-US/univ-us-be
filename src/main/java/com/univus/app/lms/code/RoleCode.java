package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 사용자 역할 코드 (MEMBER.ROLE) */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RoleCode implements CommonCode {
    SUA("SUA", "슈퍼관리자", 1),
    ADM("ADM", "관리자", 2),
    PROF("PROF", "교수", 3),
    STU("STU", "학생", 4),
    ALU("ALU", "졸업생", 5),
    GUEST("GUEST", "게스트", 6);

    private final String code;
    private final String label;
    private final int order;

    RoleCode(String code, String label, int order) {
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

    public static RoleCode fromCode(String code) {
        for (RoleCode r : values()) {
            if (r.code.equals(code)) return r;
        }
        throw new IllegalArgumentException("알 수 없는 사용자 역할 코드: " + code);
    }

}
