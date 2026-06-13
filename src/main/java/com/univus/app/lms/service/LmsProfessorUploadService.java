package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsUploadDto;

import java.util.List;

/** PLM-005 / PLM-005-01 교수 강의 자료 업로드 Service */
public interface LmsProfessorUploadService {

    /** 등록 폼 과목(강의) 드롭다운 — 교수 담당 강의 전체 */
    List<LmsUploadDto.Lecture> getLectures(Long memberId);

    /** 자료 목록 1페이지 — 서버 페이지네이션(년도/학기 필터 선택). page 0-based */
    PaginateUtilRestApiRes<LmsUploadDto.Material> getUploads(Long memberId, int page, int size, Integer year, String termCode);

    /** 목록 메타 — 전체 건수(필터 무관) + 필터 옵션(자료 보유 년도/학기) */
    LmsUploadDto.UploadMeta getUploadsMeta(Long memberId);

    /** 자료 등록 (본체 + 첨부 1건) → 생성된 자료 반환 */
    LmsUploadDto.Material createUpload(Long memberId, LmsUploadDto.CreateRequest request);

    /** 자료 수정 (제목·설명, 파일은 첨부 시에만 교체) → 갱신된 자료 반환 */
    LmsUploadDto.Material updateUpload(Long memberId, Long uploadId, LmsUploadDto.UpdateRequest request);

    /** 자료 삭제 (첨부 → 본체 물리 삭제 + 디스크 파일 정리 시도) */
    void deleteUpload(Long memberId, Long uploadId);
}
