package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfEnrolleeDto;
import com.univus.app.lms.dto.LmsProfLectureDto;
import com.univus.app.lms.dto.LmsProfSemesterDto;

import java.util.List;

public interface LmsProfEnrolleeService {

    List<LmsProfSemesterDto> getSemesters();
    List<LmsProfLectureDto> getLectures(Long professorMemberId, Long semId);
    LmsProfEnrolleeDto.ListResDto getLectureStudents(Long professorMemberId, Long lecId,
                                                     String search, String submission, String sort, String order,
                                                     int page, int size);
    byte[] exportLectureStudentsExcel(Long professorMemberId, Long lecId,
                                      String search, String submission, String sort, String order);
    LmsProfEnrolleeDto.ReportResDto getStudentReport(Long professorMemberId, Long lecId, Long studentMemberId);
}
