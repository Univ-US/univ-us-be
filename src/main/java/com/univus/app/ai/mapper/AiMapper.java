package com.univus.app.ai.mapper;

import com.univus.app.ai.dto.AiDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AiMapper {
    int insertAiLog(AiDto aiDto);
    List<AiDto> selectRecentLogs(Long memberId);
}