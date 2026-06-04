package com.univus.app.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MemberDto {
  private Long memberId;
  private Long deptId;
  private Long logId;
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
