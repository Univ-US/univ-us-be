package com.univus.app.lms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsStudentProfileResponseDto {
    private String lmsStudentProfileName;         // 이름 (읽기전용)
    private String lmsStudentProfileStudentNo;    // 학번 (파생: 입학연도 + LPAD(member_id,4))
    private String lmsStudentProfileDepartment;   // 학과 (읽기전용)
    private String lmsStudentProfilePhoneNumber;  // 휴대폰 (수정 가능)
    private String lmsStudentProfileEmail;        // 이메일 (수정 가능)
    private String lmsStudentProfileImageUrl;     // 프로필 이미지 URL
}
