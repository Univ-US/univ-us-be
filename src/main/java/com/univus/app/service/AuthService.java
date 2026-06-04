package com.univus.app.service;

import com.univus.app.domain.dto.MemberDto;
import com.univus.app.domain.dto.SignupRequestDto;
import com.univus.app.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String DEFAULT_ROLE = "USER";
  private static final String DEFAULT_STATUS = "ACTIVE";

  private final MemberMapper memberMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void signup(SignupRequestDto request) {

    if (memberMapper.existsByMemberId(request.getMemberId()) > 0) {
      throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    }

    if (memberMapper.existsByCommunityNickname(request.getCommunityNickname()) > 0) {
      throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
    }

    MemberDto member = new MemberDto();
    member.setMemberId(request.getMemberId());
    member.setDeptId(request.getDeptId());
    // 회원가입시 임시 로그ID값 채우기
    // member.setLogId(request.getLogId());
    member.setLogId(request.getMemberId());
    member.setPassword(passwordEncoder.encode(request.getPassword()));
    member.setMemberName(request.getMemberName());
    member.setRole(DEFAULT_ROLE);
    member.setPhoneNumber(request.getPhoneNumber());
    member.setGender(request.getGender());
    member.setStatus(DEFAULT_STATUS);
    member.setBirth(request.getBirth());
    member.setCommunityNickname(request.getCommunityNickname());

    memberMapper.insertMember(member);

  }

}
