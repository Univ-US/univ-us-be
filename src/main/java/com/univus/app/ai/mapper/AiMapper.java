package com.univus.app.ai.mapper;

import com.univus.app.ai.dto.AiDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiMapper {
    int insertAiLog(AiDto aiDto);
}