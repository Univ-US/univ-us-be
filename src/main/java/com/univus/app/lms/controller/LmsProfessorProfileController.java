package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsProfessorProfileResponseDto;
import com.univus.app.lms.dto.LmsProfessorProfileUpdateDto;
import com.univus.app.lms.service.LmsProfessorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lms/professor")
@RequiredArgsConstructor
public class LmsProfessorProfileController {

    private final LmsProfessorProfileService lmsProfessorProfileService;

    /** PLM-001 교수 프로필 조회 (LMS_PROFILE 없으면 지연 생성) */
    @GetMapping("/profile")
    public ResponseEntity<LmsProfessorProfileResponseDto> requestGetLmsProfessorProfile(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsProfessorProfileService.requestGetLmsProfessorProfile(memberId)); // 200
    }

    /**
     * TODO: 배포 상황에 맞춘 저장소 변경 필수
     * PLM-001 교수 프로필 수정 (multipart: 텍스트 + 이미지) */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsProfessorProfileResponseDto> requestUpdateLmsProfessorProfile(
            Authentication authentication,
            @Valid @ModelAttribute LmsProfessorProfileUpdateDto lmsProfessorProfileUpdateDto) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                lmsProfessorProfileService.requestUpdateLmsProfessorProfile(memberId, lmsProfessorProfileUpdateDto)); // 200
    }

    /** PLM-001/012 교수 회원 탈퇴 요청 */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> requestDeleteLmsProfessorProfile(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        lmsProfessorProfileService.requestDeleteLmsProfessorProfile(memberId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
