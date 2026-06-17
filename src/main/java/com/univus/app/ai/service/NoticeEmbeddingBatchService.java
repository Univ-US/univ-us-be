package com.univus.app.ai.service;

import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.mapper.AdminMapper;
import com.univus.app.ai.repository.NoticeVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeEmbeddingBatchService {

    private final AdminMapper adminMapper;
    private final EmbeddingService embeddingService;
    private final NoticeVectorRepository noticeVectorRepository;

    public int embedAll(Long univId) {
        // role=null → 전체 공지 조회 (관리자 뷰)
        List<AdminDto.NoticeListDto> notices = adminMapper.selectNoticeList(univId, null);
        if (notices == null || notices.isEmpty()) return 0;

        int success = 0;
        for (AdminDto.NoticeListDto notice : notices) {
            try {
                String text = notice.getTitle() + "\n" + (notice.getContent() != null ? notice.getContent() : "");
                float[] embedding = embeddingService.embed(text);
                noticeVectorRepository.upsert(
                        notice.getNoticeId(),
                        notice.getUnivId(),
                        notice.getTitle(),
                        notice.getContent(),
                        notice.getTarget(),
                        notice.getPostedAt(),
                        embedding
                );
                success++;
                log.info("임베딩 완료 noticeId={}", notice.getNoticeId());
            } catch (Exception e) {
                log.error("임베딩 실패 noticeId={}: {} / cause: {}", notice.getNoticeId(), e.getMessage(),
                        e.getCause() != null ? e.getCause().getMessage() : "없음");
            }
        }
        log.info("배치 완료 univId={} 성공={}/{}", univId, success, notices.size());
        return success;
    }
}
