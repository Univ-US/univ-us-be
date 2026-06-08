package com.univus.app.lms.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsStudentProfileResponseDto {
    private String lmsStudentProfileName;         // 이름 (읽기전용)
    private String lmsStudentProfileStudentNo;    // 학번 (서비스에서 입학연도+memberId로 조립)
    private String lmsStudentProfileDepartment;   // 학과 (읽기전용)
    private String lmsStudentProfilePhoneNumber;  // 휴대폰 (수정 가능)
    private String lmsStudentProfileEmail;        // 이메일 (수정 가능)
    private String lmsStudentProfileImageUrl;     // 프로필 이미지 URL
    private String lmsStudentProfileUniversityName; // 대학교명 (UNIVERSITY.UNIV_NAME)
    private String lmsStudentProfileRole;         // 역할 한글 라벨 (MEMBER.ROLE → RoleCode.label, 예: 학생)

    @JsonIgnore                                   // 학번 파생 원천(가입연도). 응답 JSON엔 노출 안 함
    private Integer admissionYear;
}
