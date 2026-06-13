package com.univus.app.commoncode.code;

/* 사용자 역할 코드 (MEMBER.ROLE) */
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

    @Override public String getCode()  { return code; }

    @Override public String getLabel() { return label; }

    @Override public int getOrder()    { return order; }

    public static RoleCode fromCode(String code) {
        for (RoleCode r : values()) {
            if (r.code.equals(code)) return r;
        }
        throw new IllegalArgumentException("알 수 없는 사용자 역할 코드: " + code);
    }

}
