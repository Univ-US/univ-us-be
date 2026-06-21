package com.univus.app.lms.service;

import com.univus.app.common.PaginateUtilRestApiRes;
import com.univus.app.lms.dto.LmsProfNoticeDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

/** PLM-007 교수 공지사항 관리 Service */
public interface LmsProfNoticeService {

    /** 공지 작성 대상 = 담당 강의 드롭다운 */
    List<LmsProfNoticeDto.LectureResDto> getLectures(Long memberId);

    /** 선택 강의(lecId)의 공지 1페이지 (최신순, 서버 페이지네이션) */
    PaginateUtilRestApiRes<LmsProfNoticeDto.NoticeResDto> getNotices(Long memberId, Long lecId, int page, int size);

    /** 공지 작성 (multipart) → 생성 공지 반환 */
    LmsProfNoticeDto.NoticeResDto createNotice(Long memberId, LmsProfNoticeDto.CreateReqDto request);

    /** 공지 수정 (강의 변경 불가, 첨부 추가 + 개별 제거) → 수정 공지 반환 */
    LmsProfNoticeDto.NoticeResDto updateNotice(Long memberId, Long noticeId, LmsProfNoticeDto.UpdateReqDto request);

    /** 공지 삭제 (첨부 → 본체 물리 삭제) */
    void deleteNotice(Long memberId, Long noticeId);

    /** 공지 첨부 다운로드 (인증 blob — 본인 강의 첨부만, 없으면 404) */
    ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId);
}
