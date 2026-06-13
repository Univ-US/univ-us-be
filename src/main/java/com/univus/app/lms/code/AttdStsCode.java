package com.univus.app.lms.code;

import com.univus.app.commoncode.code.CommonCode;

/* 출결 상태 코드 — 공통코드 그룹 ATTD_STS(출석상태) / 매핑 컬럼 STUDENT_ENROLLMENT_ATTENDANCE.STD_ENR_ATD_STS_CODE
 * ⚠️ 그룹명(ATTD_STS) ≠ 컬럼명(STD_ENR_ATD_STS_CODE). 라벨/순서는 DB COMMON_CODE(2026-06-13)와 동기화. */
public enum AttdStsCode implements CommonCode {
    PRESENT("PRS", "출석", 1),
    LATE("LAT", "지각", 2),
    ABSENT("ABS", "결석", 3),
    EARLY_LEAVE("ELV", "조퇴", 4),
    EXCUSED("EXC", "공결", 5);

    private final String code;
    private final String label;
    private final int order;

    AttdStsCode(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @Override public String getCode()  { return code; }

    @Override public String getLabel() { return label; }

    @Override public int getOrder()    { return order; }

    public static AttdStsCode fromCode(String code) {
        for (AttdStsCode s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("알 수 없는 출결 상태 코드: " + code);
    }
}
