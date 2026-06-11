package com.univus.app.admin.controller;

import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 문의 등록 (비로그인 가능)
    @PostMapping("/support")
    @ResponseStatus(HttpStatus.CREATED)
    public void createSupport(@RequestBody AdminDto.SupportRequestDto support) {
        adminService.createSupport(support);
    }

    // 문의 목록 조회 (ADM: univId 필수, SUA: univId 없으면 전체)
    @GetMapping("/support")
    public ResponseEntity<List<AdminDto.SupportListDto>> getSupportList(
            @RequestParam(required = false) Long univId,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getSupportList(memberId, univId));
    }

    // 문의 상태 변경 (ADM, SUA)
    @PatchMapping("/support/{supportId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSupportStatus(@PathVariable Long supportId,
                                    @RequestBody AdminDto.SupportStatusDto dto) {
        adminService.updateSupportStatus(supportId, dto.getStatus());
    }

    // 대학 목록 조회 (비로그인 가능)
    @GetMapping("/universities")
    public ResponseEntity<List<AdminDto.UniversityDto>> getUniversityList() {
        return ResponseEntity.ok(adminService.getUniversityList());
    }

    // 대학 단건 조회 (ADM, SUA)
    @GetMapping("/universities/{univId}")
    public ResponseEntity<AdminDto.UniversityDto> getUniversity(@PathVariable("univId") Long univId) {
        return ResponseEntity.ok(adminService.getUniversity(univId));
    }

    // 회원 목록 조회 (필터 + 페이징)
    @GetMapping("/members")
    public ResponseEntity<Map<String, Object>> getMemberList(
            @ModelAttribute AdminDto.MemberSearchDto search,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getMemberList(search, memberId));
    }

    // 회원 일괄 등록
    @PostMapping("/members/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerBulkMembers(@RequestBody AdminDto.MemberBulkRequestDto request) {
        adminService.registerBulkMembers(request.getMembers());
    }

    // 회원 상태 변경 (정지 / 탈퇴)
    @PatchMapping("/members/{memberId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberStatus(@PathVariable Long memberId,
                                   @RequestBody AdminDto.MemberStatusDto memberStatusDto) {
        memberStatusDto.setMemberId(memberId);
        adminService.updateMemberStatus(memberStatusDto);
    }

    // 공지 목록 조회
    @GetMapping("/notices")
    public ResponseEntity<List<AdminDto.NoticeListDto>> getNoticeList(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getNoticeList(memberId));
    }

    // 공지 등록
    @PostMapping("/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public void createNotice(@RequestBody AdminDto.NoticeDto notice, Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        adminService.createNotice(notice, memberId);
    }

    // 공지 수정
    @PutMapping("/notices/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNotice(@PathVariable Long noticeId,
                             @RequestBody AdminDto.NoticeDto notice) {
        notice.setNoticeId(noticeId);
        adminService.updateNotice(notice);
    }

    // 공지 삭제
    @DeleteMapping("/notices/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotice(@PathVariable Long noticeId) {
        adminService.deleteNotice(noticeId);
    }

    // 대학 링크 수정 (ADM, SUA)
    @PatchMapping("/universities/{univId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUniversity(@PathVariable Long univId,
                                 @RequestBody AdminDto.UniversityUpdateDto dto) {
        adminService.updateUniversity(univId, dto);
    }

    // 학과 목록 조회 (ADM: 본인 대학, SUA: univId로 필터)
    @GetMapping("/departments")
    public ResponseEntity<List<AdminDto.DepartmentDto>> getDepartmentList(
            @RequestParam(required = false) Long univId,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getDepartmentList(memberId, univId));
    }

    // 강의코드 목록 조회 (ADM: 본인 대학, SUA: univId로 필터)
    @GetMapping("/lecture-codes")
    public ResponseEntity<List<AdminDto.LectureCodeListDto>> getLectureCodeList(
            @RequestParam(required = false) Long univId,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getLectureCodeList(memberId, univId));
    }

    // 강의코드 등록
    @PostMapping("/lecture-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public void createLectureCode(@RequestBody AdminDto.LectureCodeDto dto) {
        adminService.createLectureCode(dto);
    }

    // 강의코드 수정
    @PutMapping("/lecture-codes/{lectureCodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLectureCode(@PathVariable Long lectureCodeId,
                                  @RequestBody AdminDto.LectureCodeDto dto) {
        dto.setLecCodeId(lectureCodeId);
        adminService.updateLectureCode(dto);
    }

    // 강의코드 상태 변경
    @PatchMapping("/lecture-codes/{lecCodeId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLectureCodeStatus(@PathVariable Long lecCodeId,
                                        @RequestBody AdminDto.LectureCodeStatusDto dto) {
        adminService.updateLectureCodeStatus(lecCodeId, dto.getValStatus());
    }

    // 강의코드 삭제
    @DeleteMapping("/lecture-codes/{lectureCodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLectureCode(@PathVariable Long lectureCodeId) {
        adminService.deleteLectureCode(lectureCodeId);
    }

    // 강의 목록 조회 (강의 관리 — 배정 강의 수 포함, 삭제(DEL) 상태도 포함 전체)
    @GetMapping("/lectures")
    public ResponseEntity<List<AdminDto.LectureListDto>> getLectureList(
            @RequestParam(required = false) Long univId,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getLectureList(memberId, univId));
    }

    // 강의 등록 (강의 관리 — 같은 학과 내 코드 중복 시 409)
    @PostMapping("/lectures")
    @ResponseStatus(HttpStatus.CREATED)
    public void createLecture(@RequestBody AdminDto.LectureCodeDto dto) {
        adminService.createLecture(dto);
    }

    // 강의 수정 (강의 관리 — 같은 학과 내 코드 중복 시 409)
    @PutMapping("/lectures/{lectureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLecture(@PathVariable Long lectureId,
                              @RequestBody AdminDto.LectureCodeDto dto) {
        dto.setLecCodeId(lectureId);
        adminService.updateLecture(dto);
    }

    // 강의 상태 변경 (강의 관리 — 강의코드 상태 변경 재사용. '삭제(DEL)'는 소프트 삭제라 배정 존재 검사 경유)
    @PatchMapping("/lectures/{lectureId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLectureStatus(@PathVariable Long lectureId,
                                    @RequestBody AdminDto.LectureCodeStatusDto dto) {
        if ("DEL".equals(dto.getValStatus())) {
            adminService.deleteLecture(lectureId);
            return;
        }
        adminService.updateLectureCodeStatus(lectureId, dto.getValStatus());
    }

    // 강의 삭제 (강의 관리 — 소프트 삭제, 배정 강의 존재 시 409)
    @DeleteMapping("/lectures/{lectureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLecture(@PathVariable Long lectureId) {
        adminService.deleteLecture(lectureId);
    }

    // 배정 강의 목록 조회 (강의 배정 — ADM: 본인 대학, SUA: univId로 필터)
    @GetMapping("/lectures/assigns")
    public ResponseEntity<List<AdminDto.LectureAssignListDto>> getLectureAssignList(
            @RequestParam(required = false) Long univId,
            @RequestParam(required = false) Long semId,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getLectureAssignList(memberId, univId, semId));
    }

    // 강의 배정 등록 (분반 자동 채번 + 요일·시간) — 생성된 배정 강의 반환
    @PostMapping("/lectures/assigns")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDto.LectureAssignListDto createLectureAssign(@RequestBody AdminDto.LectureAssignCreateDto dto) {
        return adminService.createLectureAssign(dto);
    }

    // 강의 배정 수정 (수강신청중 OPEN 상태만 — 수정된 배정 강의 반환)
    @PutMapping("/lectures/assigns/{lecId}")
    public ResponseEntity<AdminDto.LectureAssignListDto> updateLectureAssign(@PathVariable Long lecId,
                                                                             @RequestBody AdminDto.LectureAssignCreateDto dto) {
        return ResponseEntity.ok(adminService.updateLectureAssign(lecId, dto));
    }

    // 학기 목록 조회
    @GetMapping("/lectures/semesters")
    public ResponseEntity<List<AdminDto.SemesterDto>> getSemesterList() {
        return ResponseEntity.ok(adminService.getSemesterList());
    }

    // 교수 목록 조회 (ADM: 본인 대학, SUA: univId로 필터)
    @GetMapping("/lectures/professors")
    public ResponseEntity<List<AdminDto.ProfessorDto>> getProfessorList(
            @RequestParam(required = false) Long univId,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminService.getProfessorList(memberId, univId));
    }
}
