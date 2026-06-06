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
}
