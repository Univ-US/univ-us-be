package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 메세지 전송 상태코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MsgStatusCode implements CommonCode {

    SNT("SNT", "전송완료", 1),
    FAL("FAL", "전송실패", 2),
    DEL("DEL", "삭제됨",   3);

    private final String code;
    private final String label;
    private final int order;

    MsgStatusCode(String code, String label, int order) {
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

    public static MsgStatusCode fromCode(String code) {
        for (MsgStatusCode m : values()) {
            if (m.code.equals(code)) return m;
        }
        throw new IllegalArgumentException("알 수 없는 메세지 전송 상태코드: " + code);
    }

}
