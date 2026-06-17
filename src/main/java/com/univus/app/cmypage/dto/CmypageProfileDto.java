package com.univus.app.cmypage.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CmypageProfileDto {
    private Long memberId;
    private String memberName;
    private String communityNickname;
    private String univName;
    private String deptName;
    private String status;
    private LocalDateTime createdAt;
}
