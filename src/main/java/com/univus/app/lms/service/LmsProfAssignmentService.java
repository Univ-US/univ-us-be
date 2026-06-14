package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsProfAssignmentDto;

import java.util.List;

/** PLM-006 교수 과제 관리 Service — 과제 CRUD + 첨부(다중) + 제출/채점/수강생 집계 */
public interface LmsProfAssignmentService {

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    List<LmsProfAssignmentDto.LectureResDto> getLectures(Long memberId);

    /** 선택 과목 1개의 과제 1페이지 — 서버 페이지네이션. page 0-based (과목 드롭다운은 getLectures 재사용) */
    PaginateUtilRestApiRes<LmsProfAssignmentDto.AssignmentResDto> getCourseAssignments(
            Long memberId, Long lecId, int page, int size);

    /** 과제 등록 (multipart) → 생성된 과제 반환 */
    LmsProfAssignmentDto.AssignmentResDto createAssignment(Long memberId, LmsProfAssignmentDto.CreateReqDto request);

    /** 과제 수정 (multipart: 첨부 추가/개별 제거) → 갱신된 과제 반환 */
    LmsProfAssignmentDto.AssignmentResDto updateAssignment(Long memberId, Long assignmentId,
                                                 LmsProfAssignmentDto.UpdateReqDto request);

    /** 과제 삭제 (첨부 → 본체 물리 삭제 + 디스크 정리) */
    void deleteAssignment(Long memberId, Long assignmentId);
}
