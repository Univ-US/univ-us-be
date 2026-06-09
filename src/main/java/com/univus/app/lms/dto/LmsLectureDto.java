package com.univus.app.lms.dto;

import lombok.*;

/** 강의 드롭다운 + PLM-003 헤더(강의명·학기 표시) 공용 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsLectureDto {
    private Long lecId;             // LECTURE.LEC_ID
    private String lecName;         // LECTURE_CODE.LEC_COD_NAME (예: 자료구조)
    private String lecCode;         // LECTURE_CODE.LEC_CODE (예: DTST)
    private Integer lecSection;     // LECTURE.LEC_SECTION (분반)
    private Long semId;             // LECTURE.SEM_ID
    private Integer year;           // 헤더용 (드롭다운에선 null)
    private String termCode;        // 헤더용 (드롭다운에선 null)
    private String semesterDisplay; // "2026년 1학기" — 헤더에서 service 세팅
}
