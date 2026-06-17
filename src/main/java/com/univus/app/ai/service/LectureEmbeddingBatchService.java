package com.univus.app.ai.service;

import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.mapper.AdminMapper;
import com.univus.app.ai.repository.LectureVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LectureEmbeddingBatchService {

    private final AdminMapper adminMapper;
    private final EmbeddingService embeddingService;
    private final LectureVectorRepository lectureVectorRepository;

    public int embedAll(Long univId) {
        List<AdminDto.LectureAssignListDto> lectures = adminMapper.selectLectureAssignList(univId, null);
        if (lectures == null || lectures.isEmpty()) return 0;

        int success = 0;
        for (AdminDto.LectureAssignListDto lec : lectures) {
            try {
                String text = buildEmbedText(lec);
                float[] embedding = embeddingService.embed(text);
                lectureVectorRepository.upsert(
                        lec.getLecId(), univId,
                        lec.getLecCodName(), lec.getLecCode(),
                        lec.getDeptName(), lec.getProfessorName(),
                        lec.getLecCredit(), lec.getDayCodes(),
                        lec.getStartTime(), lec.getEndTime(),
                        lec.getSemYear(), lec.getSemTerm(),
                        embedding);
                log.info("강의 임베딩 완료 lecId={} name={}", lec.getLecId(), lec.getLecCodName());
                success++;
            } catch (Exception e) {
                log.error("강의 임베딩 실패 lecId={}: {} / cause: {}", lec.getLecId(), e.getMessage(),
                        e.getCause() != null ? e.getCause().getMessage() : "없음");
            }
        }
        log.info("강의 배치 완료 univId={} 성공={}/{}", univId, success, lectures.size());
        return success;
    }

    private String buildEmbedText(AdminDto.LectureAssignListDto lec) {
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
