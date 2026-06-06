package com.univus.app.lms.code;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.univus.app.common.CommonCode;

/* 첨부파일 상태코드 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT) // Enum 객체 형태 직렬화
public enum AttachmentValStatusCode implements CommonCode {
    NRM("NRM", "정상", 1),
    DEL("DEL", "삭제", 2),
    ARC("ARC", "보관", 3);

    private final String code;
    private final String label;
    private final int order;

    AttachmentValStatusCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @Override
    @JsonProperty("code") // JSON 나갈 필드
    public String getCode()   { return code; }

    @Override
    @JsonProperty("label")
    public String getLabel()  { return label; }

    @Override
    @JsonProperty("oder")
    public int getOrder() {
        return order;
    }

    public static AttachmentValStatusCode fromCode(String code) {
        for (AttachmentValStatusCode v : values()) {
            if (v.code.equals(code)) return v;
        }
        throw new IllegalArgumentException("알 수 없는 첨부파일 상태코드: " + code);
    }
}
