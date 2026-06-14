package com.univus.app.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * PLM-001 교수 프로필 DTO 묶음 (3분류 네스티드 — CLAUDE-BE §9 #12).
 * <ul>
 *   <li>바깥 = 매핑(정본) DTO: MyBatis resultType, SQL 컬럼과 1:1 (role은 코드값·라벨 변환 전).</li>
 *   <li>{@code ReqDto} = 요청(PUT multipart): FE form key = 필드명 그대로(FE 계약).</li>
 *   <li>{@code ResDto} = 응답(GET/PUT): JSON 프로퍼티명 = FE 계약(불변).</li>
 * </ul>
 * service가 매핑 DTO → ResDto 변환(role 코드→한글 라벨).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsProfProfileDto {
    // ── 매핑(정본): MyBatis가 SQL에서 채우는 raw 값 ──
    private String name;            // MEMBER.MEMBER_NAME
    private String employeeNo;      // MEMBER.LOGIN_ID (사번)
    private String department;      // DEPARTMENT.DEPT_NAME (nullable)
    private String phoneNumber;     // MEMBER.PHONE_NUMBER
    private String email;           // LMS_PROFILE.LMS_PRF_EMAIL
    private String introduction;    // LMS_PROFILE.LMS_PRF_INTRO
    private String imageUrl;        // 최신 유효 프로필 이미지 URL (nullable)
    private String universityName;  // UNIVERSITY.UNIV_NAME (nullable)
    private String role;            // MEMBER.ROLE (코드값 — ResDto 변환 시 라벨화)

    /** 요청 (PUT multipart) — FE form key = 필드명 그대로 유지 */
    @Getter
    @Setter
    public static class ReqDto {
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String lmsProfessorProfileEmail;
        @Size(max = 200, message = "소개는 200자 이내여야 합니다.")
        private String lmsProfessorProfileIntroduction;
        private MultipartFile lmsProfessorProfileImage; // 변경 없으면 null
    }

    /** 응답 (GET/PUT) — JSON 프로퍼티명 = FE 계약(불변) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResDto {
        private String lmsProfessorProfileName;
        private String lmsProfessorProfileEmployeeNo;
        private String lmsProfessorProfileDepartment;
        private String lmsProfessorProfilePhoneNumber;
        private String lmsProfessorProfileEmail;
        private String lmsProfessorProfileIntroduction;
        private String lmsProfessorProfileImageUrl;
        private String lmsProfessorProfileUniversityName;
        private String lmsProfessorProfileRole;
    }
}
