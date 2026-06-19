package com.univus.app.lms.dto;

import lombok.*;

/** 강의 드롭다운 + PLM-003 헤더(강의명·학기 표시) 공용 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsProfLectureDto {
    private Long lecId;             // LECTURE.LEC_ID
    private String lecName;         // LECTURE_CODE.LEC_COD_NAME (예: 자료구조)
    private String lecCode;         // LECTURE_CODE.LEC_CODE (예: DTST)
    private Integer lecSection;     // LECTURE.LEC_SECTION (분반)
    private Long semId;             // LECTURE.SEM_ID
    private String lecValStatus;    // LECTURE.LEC_VAL_STATUS (OPEN/PROG/CLSD/CNCL) — FE가 공통코드 LEC_VAL_STATUS로 라벨
    private Integer semYear;        // SEMESTERS.SEM_YEAR — 학기 연도 (드롭다운·헤더 모두 반환)
    private String semTerm;         // SEMESTERS.SEM_TERM — 학기 코드 (드롭다운·헤더 모두 반환, 공통코드로 라벨)
    private String semesterDisplay; // 미사용 (서버 미설정, 항상 null)
}
