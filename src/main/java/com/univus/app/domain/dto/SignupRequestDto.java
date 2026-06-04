package com.univus.app.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto {

  private Long memberId;
  private Long deptId;
  // 회원가입시 로그ID = 멤버ID로 임시 처리
  // private Long logId;
  private String password;
  private String memberName;
  private Long phoneNumber;
  private String gender;
  private String birth;
  private String communityNickname;

}
