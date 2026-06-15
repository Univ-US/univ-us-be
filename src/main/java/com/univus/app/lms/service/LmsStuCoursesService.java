package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuCoursesDto;

import java.util.List;

public interface LmsStuCoursesService {

    List<LmsStuCoursesDto.SemesterCoursesResDto> getCourses(Long memberId);
}
