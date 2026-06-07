package com.univus.app.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class LmsStudentProfileUpdateDto {
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String lmsStudentProfileEmail;
    private MultipartFile lmsStudentProfileImage; // 변경 없으면 null
}
