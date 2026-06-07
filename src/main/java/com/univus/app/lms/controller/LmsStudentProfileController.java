package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStudentProfileResponseDto;
import com.univus.app.lms.dto.LmsStudentProfileUpdateDto;
import com.univus.app.lms.service.LmsStudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lms/student")
@RequiredArgsConstructor
public class LmsStudentProfileController {

    private final LmsStudentProfileService lmsStudentProfileService;

    /** SLM-001 학생 프로필 조회 (LMS_PROFILE 없으면 지연 생성) */
    @GetMapping("/profile")
    public ResponseEntity<LmsStudentProfileResponseDto> requestGetLmsStudentProfile(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStudentProfileService.requestGetLmsStudentProfile(memberId)); // 200
    }

    /**
     * TODO: 배포 시 저장 경로 설정 필수
     * SLM-001 학생 프로필 수정 (multipart: 텍스트 + 이미지) */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsStudentProfileResponseDto> requestUpdateLmsStudentProfile(
            Authentication authentication,
            @Valid @ModelAttribute LmsStudentProfileUpdateDto dto) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStudentProfileService.requestUpdateLmsStudentProfile(memberId, dto)); // 200
    }

    /** SLM-001/012 학생 회원 탈퇴 요청 */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> requestDeleteLmsStudentProfile(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        lmsStudentProfileService.requestDeleteLmsStudentProfile(memberId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
