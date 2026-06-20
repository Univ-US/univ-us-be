package com.univus.app.ai.service;

import com.univus.app.ai.dto.AiDto;
import jakarta.servlet.http.HttpServletResponse;

public interface AiService {

    void chat(AiDto aiDto, HttpServletResponse response) throws Exception;
}
