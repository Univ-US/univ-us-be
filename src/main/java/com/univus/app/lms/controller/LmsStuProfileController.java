package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuProfileDto;
import com.univus.app.lms.service.LmsStuProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lms/student")
@RequiredArgsConstructor
public class LmsStuProfileController {

    private final LmsStuProfileService lmsStuProfileService;

    /** SLM-001 학생 프로필 조회 (LMS_PROFILE 없으면 지연 생성) */
    // GET /api/lms/student/profile
    @GetMapping("/profile")
    public ResponseEntity<LmsStuProfileDto.ResDto> requestGetLmsStudentProfile(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuProfileService.requestGetLmsStudentProfile(memberId)); // 200
    }

    /** SLM-001 학생 프로필 수정 (multipart: 텍스트 + 이미지) */
    // PUT /api/lms/student/profile (multipart/form-data)
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsStuProfileDto.ResDto> requestUpdateLmsStudentProfile(
            Authentication authentication,
            @Valid @ModelAttribute LmsStuProfileDto.ReqDto dto) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuProfileService.requestUpdateLmsStudentProfile(memberId, dto)); // 200
    }
}
