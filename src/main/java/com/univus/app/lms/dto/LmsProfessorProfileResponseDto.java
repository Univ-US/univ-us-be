package com.univus.app.lms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsProfessorProfileResponseDto {
    private String lmsProfessorProfileName;
    private String lmsProfessorProfileDepartment;
    private String lmsProfessorProfilePhoneNumber;
    private String lmsProfessorProfileEmail;
    private String lmsProfessorProfileIntroduction;
    private String lmsProfessorProfileImageUrl;
    private String lmsProfessorProfileUniversityName;  // 대학교명 (UNIVERSITY.UNIV_NAME)
    private String lmsProfessorProfileRole;            // 역할 한글 라벨 (MEMBER.ROLE → RoleCode.label, 예: 교수)
}
