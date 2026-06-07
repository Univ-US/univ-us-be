package com.univus.app.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class LmsProfessorProfileUpdateDto {
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String lmsProfessorProfileEmail;
    @Size(max = 200, message = "소개는 200자 이내여야 합니다.")
    private String lmsProfessorProfileIntroduction;
    private MultipartFile lmsProfessorProfileImage; // no chane: null
}
