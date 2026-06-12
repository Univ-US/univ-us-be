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

//TODO: SpringSequerity ROLE 필터 추가
@RestController
@RequestMapping("/api/lms/professor")
@RequiredArgsConstructor
public class LmsProfessorProfileController {

    private final LmsProfessorProfileService lmsProfessorProfileService;

    /** PLM-001 교수 프로필 조회 (LMS_PROFILE 없으면 지연 생성) */
    //TODO: 멤버 테이블에 이메일이랑 학번(ID) 생기면 로직 변경
    // GET /api/lms/professor/profile
    @GetMapping("/profile")
    public ResponseEntity<LmsProfessorProfileResponseDto> requestGetLmsProfessorProfile(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsProfessorProfileService.requestGetLmsProfessorProfile(memberId)); // 200
    }

    /** PLM-001 교수 프로필 수정 (multipart: 텍스트 + 이미지) */
    //TODO: 멤버 테이블에 이메일이랑 학번(ID) 생기면 로직 변경
    // PUT /api/lms/professor/profile (multipart/form-data)
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsProfessorProfileResponseDto> requestUpdateLmsProfessorProfile(
            Authentication authentication,
            @Valid @ModelAttribute LmsProfessorProfileUpdateDto lmsProfessorProfileUpdateDto) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(
                lmsProfessorProfileService.requestUpdateLmsProfessorProfile(memberId, lmsProfessorProfileUpdateDto)); // 200
    }

    /** PLM-001/012 교수 회원 탈퇴 요청 */
    // DELETE /api/lms/professor/profile
    @DeleteMapping("/profile")
    public ResponseEntity<Void> requestDeleteLmsProfessorProfile(Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        lmsProfessorProfileService.requestDeleteLmsProfessorProfile(memberId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
