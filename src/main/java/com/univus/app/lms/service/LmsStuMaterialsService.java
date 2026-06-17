package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuMaterialsDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LmsStuMaterialsService {

    List<LmsStuMaterialsDto.SemesterMaterialsResDto> getMaterials(Long memberId);

    ResponseEntity<?> downloadAttachment(Long memberId, Long attachmentId);
}
