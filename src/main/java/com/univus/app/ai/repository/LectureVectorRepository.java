package com.univus.app.ai.repository;

import com.univus.app.ai.dto.LectureVectorDto;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;

@Repository
public class LectureVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public LectureVectorRepository(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(Long lecId, Long univId, String lecCodName, String lecCode,
                       String deptName, String professorName, Integer lecCredit,
                       String dayCodes, String startTime, String endTime,
                       Integer semYear, String semTerm, float[] embedding) {
        jdbcTemplate.update("""
                INSERT INTO lecture_embedding
                    (lec_id, univ_id, lec_cod_name, lec_code, dept_name, professor_name,
                     lec_credit, day_codes, start_time, end_time, sem_year, sem_term, embedding, synced_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (lec_id) DO UPDATE
                    SET lec_cod_name   = EXCLUDED.lec_cod_name,
                        lec_code       = EXCLUDED.lec_code,
                        dept_name      = EXCLUDED.dept_name,
                        professor_name = EXCLUDED.professor_name,
                        lec_credit     = EXCLUDED.lec_credit,
                        day_codes      = EXCLUDED.day_codes,
                        start_time     = EXCLUDED.start_time,
                        end_time       = EXCLUDED.end_time,
                        sem_year       = EXCLUDED.sem_year,
                        sem_term       = EXCLUDED.sem_term,
                        embedding      = EXCLUDED.embedding,
                        synced_at      = NOW()
                """,
                lecId, univId, lecCodName, lecCode, deptName, professorName,
                lecCredit, dayCodes, startTime, endTime, semYear, semTerm,
                toPGvector(embedding));
    }

    public List<LectureVectorDto> findSimilar(Long univId, float[] queryEmbedding, int limit) {
        return jdbcTemplate.query(
                "SELECT lec_id, univ_id, lec_cod_name, lec_code, dept_name, professor_name, " +
                "lec_credit, day_codes, start_time, end_time, sem_year, sem_term, " +
                "1 - (embedding <=> ?) AS similarity " +
                "FROM lecture_embedding " +
                "WHERE univ_id = ? " +
                "ORDER BY embedding <=> ? " +
                "LIMIT ?",
                (rs, rowNum) -> {
                    LectureVectorDto dto = new LectureVectorDto();
                    dto.setLecId(rs.getLong("lec_id"));
                    dto.setUnivId(rs.getLong("univ_id"));
                    dto.setLecCodName(rs.getString("lec_cod_name"));
                    dto.setLecCode(rs.getString("lec_code"));
                    dto.setDeptName(rs.getString("dept_name"));
                    dto.setProfessorName(rs.getString("professor_name"));
                    dto.setLecCredit(rs.getInt("lec_credit"));
                    dto.setDayCodes(rs.getString("day_codes"));
                    dto.setStartTime(rs.getString("start_time"));
                    dto.setEndTime(rs.getString("end_time"));
                    dto.setSemYear(rs.getInt("sem_year"));
                    dto.setSemTerm(rs.getString("sem_term"));
                    dto.setSimilarity(rs.getDouble("similarity"));
                    return dto;
                },
                toPGvector(queryEmbedding), univId, toPGvector(queryEmbedding), limit
        );
    }

    private PGobject toPGvector(float[] embedding) {
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < embedding.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(embedding[i]);
            }
            sb.append("]");
            PGobject pgObj = new PGobject();
            pgObj.setType("vector");
            pgObj.setValue(sb.toString());
            return pgObj;
        } catch (SQLException e) {
            throw new RuntimeException("vector PGobject 생성 실패", e);
        }
    }
}
