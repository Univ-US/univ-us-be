package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsLectureDto;
import com.univus.app.lms.dto.LmsLectureStudentsResponseDto;
import com.univus.app.lms.dto.LmsSemesterResponseDto;
import com.univus.app.lms.dto.LmsStudentReportDto;

import java.util.List;

public interface LmsProfessorEnrolleeService {

    List<LmsSemesterResponseDto> getSemesters();
    List<LmsLectureDto> getLectures(Long professorMemberId, Long semId);
    LmsLectureStudentsResponseDto getLectureStudents(Long professorMemberId, Long lecId, String search);
    LmsStudentReportDto getStudentReport(Long professorMemberId, Long lecId, Long studentMemberId);
}
