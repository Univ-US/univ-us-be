package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto {

  private String loginId;
  private String password;
  private String memberName;
  private Long phoneNumber;
  private String gender;
  private String birth;

}
