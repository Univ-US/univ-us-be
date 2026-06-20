package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsStuNoticeDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LmsStuNoticeService {

    /** 수강 과목(강의) 드롭다운 */
    List<LmsStuNoticeDto.LectureResDto> getLectures(Long memberId);

    /** 선택 과목 공지 1페이지 (서버 페이지네이션) */
    PaginateUtilRestApiRes<LmsStuNoticeDto.NoticeResDto> getNotices(
            Long memberId, Long lecId, int page, int size);

    ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId);
}
