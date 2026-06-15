package com.univus.app.lms.controller;

import com.univus.app.lms.dto.LmsStuCoursesDto;
import com.univus.app.lms.service.LmsStuCoursesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lms/student")
@RequiredArgsConstructor
public class LmsStuCoursesController {

    private final LmsStuCoursesService lmsStuCoursesService;

    @GetMapping("/courses")
    public ResponseEntity<List<LmsStuCoursesDto.SemesterCoursesResDto>> requestGetStudentCourses(
            Authentication authentication) {
        Long memberId = Long.valueOf(authentication.getPrincipal().toString());
        return ResponseEntity.ok(lmsStuCoursesService.getCourses(memberId));
    }
}
