package com.univus.app.lms.mapper;

import com.univus.app.lms.dto.LmsStuEnrollDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LmsStuEnrollMapper {

    Long findLmsPrfIdByMemberId(@Param("memberId") Long memberId);

    /** 학생의 소속 대학 (SEMESTERS가 대학 구분 없는 전역 테이블이라 강좌 조회 전체에 스코프 필수) */
    Long findUnivIdByMemberId(@Param("memberId") Long memberId);

    /** 수강신청 대상 학기 (해당 대학의 개설(OPEN) 강좌가 속한 학기 중 가장 빠른 학기). 없으면 null */
    LmsStuEnrollDto.TargetSemesterRow selectTargetSemester(@Param("univId") Long univId);

    /** 대상 학기의 개설 강좌 flat 행 (시간표 join, 미페이징, 같은 대학 강좌만) */
    List<LmsStuEnrollDto.LectureFlatRow> selectOpenLecturesFlat(@Param("semId") Long semId,
                                                                 @Param("lmsPrfId") Long lmsPrfId,
                                                                 @Param("univId") Long univId);

    /** 신청 처리용 강좌 단건 조회 (정원/학점/상태/소속 대학 검증) */
    LmsStuEnrollDto.LectureForEnrollRow selectLectureForEnroll(@Param("lecId") Long lecId);

    /** 정원 동시성 제어 — 강좌 row를 잠궈 같은 강좌에 대한 동시 신청을 직렬화 (스키마 변경 없음) */
    Integer lockLectureForEnroll(@Param("lecId") Long lecId);

    /** 강좌 시간표 슬롯 (충돌 검사용) */
    List<LmsStuEnrollDto.TimeSlotRow> selectLectureTimeSlots(@Param("lecId") Long lecId);

    /** 학생의 기존 신청 내역 단건 (LMS_PRF_ID + LEC_ID, 재신청 판별용) */
    LmsStuEnrollDto.EnrollmentRow findEnrollmentRow(@Param("lmsPrfId") Long lmsPrfId, @Param("lecId") Long lecId);

    /** 학생이 해당 학기에 현재 신청중인(DRP 제외) 학점 합 */
    int sumActiveCredits(@Param("lmsPrfId") Long lmsPrfId, @Param("semId") Long semId);

    /** 학생이 해당 학기에 현재 신청중인(DRP 제외) 강좌들의 시간표 슬롯 (충돌 검사 기준 집합) */
    List<LmsStuEnrollDto.TimeSlotRow> selectActiveScheduleRows(@Param("lmsPrfId") Long lmsPrfId,
                                                                @Param("semId") Long semId);

    /** 신규 신청 등록. 정원 초과 시 0행(INSERT 미실행) — 영향받은 행 수 반환 */
    int insertEnrollment(@Param("lmsPrfId") Long lmsPrfId, @Param("lecId") Long lecId);

    /** 취소(DRP) 되어있던 신청 내역 재활성화. 정원 초과 시 0행 — 영향받은 행 수 반환 */
    int reactivateEnrollment(@Param("lecStdEnrId") Long lecStdEnrId);

    /** 신청 취소 (ENR -> DRP). 영향받은 행 수 반환 (0이면 대상 없음) */
    int cancelEnrollment(@Param("lmsPrfId") Long lmsPrfId, @Param("lecId") Long lecId);
}
