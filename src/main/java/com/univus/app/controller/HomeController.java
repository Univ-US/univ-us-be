package com.univus.app.controller;

import com.univus.app.admin.dto.AdminDto;
import com.univus.app.admin.mapper.AdminMapper;
import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final MemberMapper memberMapper;
    private final AdminMapper adminMapper;

    @GetMapping("/profile")
    public ResponseEntity<HomeProfileResponseDto> getHomeProfile(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        MemberDto member = memberMapper.findByMemberId(memberId);
        AdminDto.UniversityDto univ = adminMapper.selectUniversityById(member.getUnivId());

        HomeProfileResponseDto response = new HomeProfileResponseDto();
        response.setUnivId(univ.getUnivId());
        response.setSchoolName(univ.getUnivName());
        response.setPhoneNumber(univ.getSchoolPhone());
        response.setYoutubeUrl(univ.getYoutubeUrl());
        response.setClubUrl(univ.getClubUrl());
        response.setSnsUrl(univ.getSnsUrl());

        return ResponseEntity.ok(response);
    }
}
