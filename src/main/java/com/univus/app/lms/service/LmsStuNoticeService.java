package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuNoticeDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LmsStuNoticeService {

    List<LmsStuNoticeDto.NoticeResDto> getNotices(Long memberId);

    ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId);
}
