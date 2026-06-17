package com.univus.app.ai.repository;

import com.univus.app.ai.dto.NoticeVectorDto;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class NoticeVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public NoticeVectorRepository(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(Long noticeId, Long univId, String title, String content,
                       String target, LocalDateTime postedAt, float[] embedding) {
        jdbcTemplate.update("""
                INSERT INTO notice_embedding
                    (notice_id, univ_id, title, content, target, posted_at, embedding, synced_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (notice_id) DO UPDATE
                    SET title     = EXCLUDED.title,
                        content   = EXCLUDED.content,
                        target    = EXCLUDED.target,
                        posted_at = EXCLUDED.posted_at,
                        embedding = EXCLUDED.embedding,
                        synced_at = NOW()
                """,
                noticeId, univId, title, content, target,
                postedAt != null ? Timestamp.valueOf(postedAt) : null,
                toPGvector(embedding));
    }

    public List<NoticeVectorDto> findSimilar(Long univId, float[] queryEmbedding, String role, int limit) {
        String targetClause = buildTargetClause(role);
        return jdbcTemplate.query(
                "SELECT notice_id, univ_id, title, content, target, posted_at, " +
                "1 - (embedding <=> ?) AS similarity " +
                "FROM notice_embedding " +
                "WHERE univ_id = ? " + targetClause +
                "ORDER BY embedding <=> ? " +
                "LIMIT ?",
                (rs, rowNum) -> {
                    NoticeVectorDto dto = new NoticeVectorDto();
                    dto.setNoticeId(rs.getLong("notice_id"));
                    dto.setUnivId(rs.getLong("univ_id"));
                    dto.setTitle(rs.getString("title"));
                    dto.setContent(rs.getString("content"));
                    dto.setTarget(rs.getString("target"));
                    Timestamp ts = rs.getTimestamp("posted_at");
                    if (ts != null) dto.setPostedAt(ts.toLocalDateTime());
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

    private String buildTargetClause(String role) {
        if (role == null || "ADM".equals(role)) return "";
        if ("PROF".equals(role)) return "AND (target = 'PROF' OR target = 'ALL') ";
        return "AND (target = 'STU' OR target = 'ALL') ";
    }
}
