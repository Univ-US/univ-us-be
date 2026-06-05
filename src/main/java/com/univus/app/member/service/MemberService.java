package com.univus.app.member.service;

import com.univus.app.member.dto.MemberDto;
import com.univus.app.member.dto.MemberResponseDto;
import com.univus.app.member.dto.SignupRequestDto;
import com.univus.app.member.exception.DuplicateMemberException;
import com.univus.app.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

  private static final String DEFAULT_ROLE = "GUEST";
  private static final String DEFAULT_STATUS = "ACTIVE";

  private final MemberMapper memberMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void signup(SignupRequestDto request) {

    if (memberMapper.existsByMemberId(request.getMemberId()) > 0) {
      throw new DuplicateMemberException("이미 사용 중인 아이디입니다.");
    }

    MemberDto member = new MemberDto();
    member.setMemberId(request.getMemberId());
    member.setPassword(passwordEncoder.encode(request.getPassword()));
    member.setMemberName(request.getMemberName());
    member.setRole(DEFAULT_ROLE);
    member.setPhoneNumber(request.getPhoneNumber());
    member.setGender(request.getGender());
    member.setStatus(DEFAULT_STATUS);
    member.setBirth(request.getBirth());

    int inserted = memberMapper.insertMember(member);

    if (inserted != 1) {
      throw new IllegalStateException("회원가입 처리에 실패했습니다.");
    }
  }

  // 응답용
  private MemberResponseDto toResponse(MemberDto member) {
    MemberResponseDto response = new MemberResponseDto();
    response.setMemberId(member.getMemberId());
    response.setUnivId(member.getUnivId());
    response.setDeptId(member.getDeptId());
    response.setMemberName(member.getMemberName());
    response.setRole(member.getRole());
    response.setPhoneNumber(member.getPhoneNumber());
    response.setGender(member.getGender());
    response.setStatus(member.getStatus());
    response.setCreatedAt(member.getCreatedAt());
    response.setLogtimeAt(member.getLogtimeAt());
    response.setBirth(member.getBirth());
    response.setCommunityNickname(member.getCommunityNickname());
    return response;
  }

}
