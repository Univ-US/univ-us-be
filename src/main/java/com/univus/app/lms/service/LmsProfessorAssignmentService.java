package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsAssignmentDto;

import java.util.List;

/** PLM-006 교수 과제 관리 Service — 과제 CRUD + 첨부(다중) + 제출/채점/수강생 집계 */
public interface LmsProfessorAssignmentService {

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    List<LmsAssignmentDto.Lecture> getLectures(Long memberId);

    /** 과제 목록 — 담당 강의 전체 과제 + 집계 + 첨부 (그룹핑은 FE 클라이언트) */
    List<LmsAssignmentDto.Assignment> getAssignments(Long memberId);

    /** 과제 등록 (multipart) → 생성된 과제 반환 */
    LmsAssignmentDto.Assignment createAssignment(Long memberId, LmsAssignmentDto.CreateRequest request);

    /** 과제 수정 (multipart: 첨부 추가/개별 제거) → 갱신된 과제 반환 */
    LmsAssignmentDto.Assignment updateAssignment(Long memberId, Long assignmentId,
                                                 LmsAssignmentDto.UpdateRequest request);

    /** 과제 삭제 (첨부 → 본체 물리 삭제 + 디스크 정리) */
    void deleteAssignment(Long memberId, Long assignmentId);
}
