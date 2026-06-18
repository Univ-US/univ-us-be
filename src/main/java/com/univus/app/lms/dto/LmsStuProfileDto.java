package com.univus.app.lms.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * SLM-001 학생 프로필 DTO 묶음 (3분류 네스티드 — CLAUDE-BE §9 #12).
 * <ul>
 *   <li>바깥 = 매핑(정본) DTO: MyBatis resultType (자기 테이블=컬럼 카멜, 조인=의미별칭).</li>
 *   <li>{@code ReqDto} = 요청(PUT multipart): FE form key = 필드명(LMS_PROFILE 자기컬럼=컬럼 카멜).</li>
 *   <li>{@code ResDto} = 응답(GET/PUT): JSON 프로퍼티명 = DB 컬럼 카멜(조인=의미별칭). role은 라벨 변환 후.</li>
 * </ul>
 * service가 매핑 DTO → ResDto 변환(role 코드→한글 라벨).
 * ※ 명명 규칙: BE 정본 + DTO 변수명 = DB 컬럼명 카멜(조인 컬럼은 의미 별칭) — 커뮤니티 PostDto 컨벤션. PLM-001 교수 프로필과 동일 정본.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LmsStuProfileDto {
    // ── 매핑(정본): MyBatis가 SQL에서 채우는 raw 값 ──
    private String name;            // MEMBER.MEMBER_NAME
    private String studentNo;       // MEMBER.LOGIN_ID (학번)
    private String department;      // DEPARTMENT.DEPT_NAME (nullable)
    private String phoneNumber;     // MEMBER.PHONE_NUMBER (조인)
    private String lmsPrfEmail;     // LMS_PROFILE.LMS_PRF_EMAIL (자기 테이블 컬럼)
    private String imageUrl;        // 최신 유효 프로필 이미지 URL (서브쿼리, nullable)
    private String universityName;  // UNIVERSITY.UNIV_NAME (nullable)
    private String role;            // MEMBER.ROLE (코드값 — ResDto 변환 시 라벨화)

    /** 요청 (PUT multipart) — FE form key = 필드명 그대로 유지 */
    @Getter
    @Setter
    public static class ReqDto {
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String lmsPrfEmail;     // LMS_PROFILE.LMS_PRF_EMAIL
        private MultipartFile image;    // 새 프로필 이미지 (변경 없으면 null)
    }

    /** 응답 (GET/PUT) — JSON 프로퍼티명 = FE 계약(불변) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResDto {
        private String name;            // MEMBER.MEMBER_NAME
        private String studentNo;       // MEMBER.LOGIN_ID (학번)
        private String department;      // DEPARTMENT.DEPT_NAME
        private String phoneNumber;     // MEMBER.PHONE_NUMBER
        private String lmsPrfEmail;     // LMS_PROFILE.LMS_PRF_EMAIL
        private String imageUrl;        // 프로필 이미지 URL
        private String universityName;  // UNIVERSITY.UNIV_NAME
        private String role;            // MEMBER.ROLE → 한글 라벨
    }
}
