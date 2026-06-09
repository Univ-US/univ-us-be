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

//TODO: SpringSequerity ROLE 필터 추가
@RestController
@RequestMapping("/api/lms/student")
@RequiredArgsConstructor
public class LmsStudentProfileController {

    private final LmsStudentProfileService lmsStudentProfileService;

    /** SLM-001 학생 프로필 조회 (LMS_PROFILE 없으면 지연 생성) */
    //TODO: 멤버 테이블에 이메일이랑 학번(ID) 생기면 로직 변경
    // GET /api/lms/student/profile
    @GetMapping("/profile")
    public ResponseEntity<LmsStudentProfileResponseDto> requestGetLmsStudentProfile(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStudentProfileService.requestGetLmsStudentProfile(memberId)); // 200
    }

    /** SLM-001 학생 프로필 수정 (multipart: 텍스트 + 이미지) */
    //TODO: 멤버 테이블에 이메일이랑 학번(ID) 생기면 로직 변경
    // PUT /api/lms/student/profile (multipart/form-data)
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsStudentProfileResponseDto> requestUpdateLmsStudentProfile(
            Authentication authentication,
            @Valid @ModelAttribute LmsStudentProfileUpdateDto dto) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(lmsStudentProfileService.requestUpdateLmsStudentProfile(memberId, dto)); // 200
    }

    /** SLM-001/012 학생 회원 탈퇴 요청 */
    // DELETE /api/lms/student/profile
    @DeleteMapping("/profile")
    public ResponseEntity<Void> requestDeleteLmsStudentProfile(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        lmsStudentProfileService.requestDeleteLmsStudentProfile(memberId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
