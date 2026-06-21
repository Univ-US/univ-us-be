package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuMaterialsDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LmsStuMaterialsService {

    /** 수강 과목(강의) 드롭다운 */
    List<LmsStuMaterialsDto.LectureResDto> getLectures(Long memberId);

    /** 선택 과목 자료 1페이지 (서버 페이지네이션) */
    PaginateUtilRestApiRes<LmsStuMaterialsDto.MaterialResDto> getMaterials(
            Long memberId, Long lecId, int page, int size);

    ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId);
}
