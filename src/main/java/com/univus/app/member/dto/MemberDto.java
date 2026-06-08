package com.univus.app.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MemberDto {
  private Long memberId;
  private String loginId;
  private Long univId;
  private Long deptId;
  private String password;
  private String memberName;
  private String role;
  private Long phoneNumber;
  private String gender;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime logtimeAt;
  private String birth;
  private String communityNickname;
}
