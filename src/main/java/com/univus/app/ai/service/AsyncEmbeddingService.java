package com.univus.app.ai.service;

import com.univus.app.admin.dto.AdminDto;
import com.univus.app.ai.repository.LectureVectorRepository;
import com.univus.app.ai.repository.NoticeVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEmbeddingService {

    private final EmbeddingService embeddingService;
    private final NoticeVectorRepository noticeVectorRepository;
    private final LectureVectorRepository lectureVectorRepository;

    @Async
    public void embedNotice(AdminDto.NoticeDto notice) {
        try {
            String text = notice.getTitle() + "\n" + (notice.getContent() != null ? notice.getContent() : "");
            float[] embedding = embeddingService.embed(text);
            noticeVectorRepository.upsert(
                    notice.getNoticeId(), notice.getUnivId(),
                    notice.getTitle(), notice.getContent(),
                    notice.getTarget(), null, embedding);
            log.info("공지 비동기 임베딩 완료 noticeId={}", notice.getNoticeId());
        } catch (Exception e) {
            log.warn("공지 비동기 임베딩 실패 noticeId={}: {}", notice.getNoticeId(), e.getMessage());
        }
    }

    @Async
    public void embedLecture(AdminDto.LectureAssignListDto lec) {
        try {
            String text = buildLectureText(lec);
            float[] embedding = embeddingService.embed(text);
            lectureVectorRepository.upsert(
                    lec.getLecId(), lec.getUnivId(),
                    lec.getLecCodName(), lec.getLecCode(),
                    lec.getDeptName(), lec.getProfessorName(),
                    lec.getLecCredit(), lec.getDayCodes(),
                    lec.getStartTime(), lec.getEndTime(),
                    lec.getSemYear(), lec.getSemTerm(),
                    embedding);
            log.info("강의 비동기 임베딩 완료 lecId={}", lec.getLecId());
        } catch (Exception e) {
            log.warn("강의 비동기 임베딩 실패 lecId={}: {}", lec.getLecId(), e.getMessage());
        }
    }

    private String buildLectureText(AdminDto.LectureAssignListDto lec) {
        StringBuilder sb = new StringBuilder();
        sb.append("강의명: ").append(lec.getLecCodName());
        if (lec.getDeptName() != null) sb.append("\n학과: ").append(lec.getDeptName());
        if (lec.getProfessorName() != null) sb.append("\n교수: ").append(lec.getProfessorName());
        if (lec.getLecCredit() != null) sb.append("\n학점: ").append(lec.getLecCredit());
        if (lec.getSemYear() != null && lec.getSemTerm() != null)
            sb.append("\n학기: ").append(lec.getSemYear()).append("년 ").append(lec.getSemTerm());
        if (lec.getDayCodes() != null) sb.append("\n요일: ").append(lec.getDayCodes());
        if (lec.getStartTime() != null && lec.getEndTime() != null)
            sb.append("\n시간: ").append(lec.getStartTime()).append("~").append(lec.getEndTime());
        return sb.toString();
    }
}
