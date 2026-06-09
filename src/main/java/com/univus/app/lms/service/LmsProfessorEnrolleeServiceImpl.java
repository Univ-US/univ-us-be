package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsAssignmentScoreDto;
import com.univus.app.lms.dto.LmsLectureDto;
import com.univus.app.lms.dto.LmsLectureStudentsResponseDto;
import com.univus.app.lms.dto.LmsSemesterResponseDto;
import com.univus.app.lms.dto.LmsStudentReportDto;
import com.univus.app.lms.dto.LmsStudentRowDto;
import com.univus.app.lms.dto.LmsStudentStatsSummaryDto;
import com.univus.app.lms.mapper.LmsProfessorStudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* PLM-003 교수 페이지 - 수강생 현황 ServiceImpl */
@Service
@RequiredArgsConstructor
public class LmsProfessorEnrolleeServiceImpl implements LmsProfessorEnrolleeService {

    /** 미제출 코드 (공통코드 LEC_ASN_SBM_STATUS = NSB). 코드→라벨은 FE가 공통코드 API로 매핑 */
    private static final String NOT_SUBMITTED_CODE = "NSB";

    private final LmsProfessorStudentMapper lmsProfessorStudentMapper;

    /** 학기 드롭다운 (코드만 반환 — 라벨은 FE가 /api/common-codes/SEM_TERM 로 매핑) */
    @Override
    @Transactional(readOnly = true)
    public List<LmsSemesterResponseDto> getSemesters() {
        return lmsProfessorStudentMapper.selectSemesters();
    }

    /** 교수 담당 강의 드롭다운 (학기 선택 optional) */
    @Override
    @Transactional(readOnly = true)
    public List<LmsLectureDto> getLectures(Long professorMemberId, Long semId) {
        Long lmsPrfId = lmsProfessorStudentMapper.findLmsPrfIdByMemberId(professorMemberId);
        if (lmsPrfId == null) {
            return List.of();
        }
        return lmsProfessorStudentMapper.selectLecturesByProfessor(lmsPrfId, semId);
    }

    /** PLM-003 : 강의별 수강생 목록 + 통계 카드 3개 */
    @Override
    @Transactional(readOnly = true)
    public LmsLectureStudentsResponseDto getLectureStudents(Long professorMemberId, Long lecId, String search) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(professorMemberId);

        LmsLectureDto lecture = lmsProfessorStudentMapper.selectLectureHeader(lecId, professorLmsPrfId);
        if (lecture == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 담당하는 강의가 아닙니다.");
        }

        int totalAssignments = lmsProfessorStudentMapper.countAssignments(lecId);
        List<LmsStudentRowDto> students =
                lmsProfessorStudentMapper.selectStudentRows(lecId, professorLmsPrfId, blankToNull(search));
        for (LmsStudentRowDto student : students) {
            student.setTotalAssignments(totalAssignments);
            student.setAttendanceRate(percent(student.getPresentCount(), student.getAttendanceTotal()));
        }

        return LmsLectureStudentsResponseDto.builder()
                .lecture(lecture)
                .summary(buildSummary(students, totalAssignments))
                .students(students)
                .build();
    }

    /** PLM-003-01 : 수강생 상세 리포트 */
    @Override
    @Transactional(readOnly = true)
    public LmsStudentReportDto getStudentReport(Long professorMemberId, Long lecId, Long studentMemberId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(professorMemberId);

        LmsStudentReportDto report =
                lmsProfessorStudentMapper.selectStudentReportHeader(lecId, professorLmsPrfId, studentMemberId);
        if (report == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 강의의 수강생을 찾을 수 없습니다.");
        }

        report.setTotalAssignments(lmsProfessorStudentMapper.countAssignments(lecId));
        report.setAttendanceRate(percent(report.getAttendancePresent(), report.getAttendanceTotal()));

        List<LmsAssignmentScoreDto> assignmentScores =
                lmsProfessorStudentMapper.selectAssignmentScores(lecId, report.getStudentLmsPrfId());
        for (LmsAssignmentScoreDto score : assignmentScores) {
            score.setScored(score.getScore() != null);
            // 제출 여부 = 미제출(NSB) 이외. 제출상태 라벨은 FE가 공통코드(LEC_ASN_SBM_STATUS)로 매핑
            score.setSubmitted(score.getSubmissionStatusCode() != null
                    && !NOT_SUBMITTED_CODE.equals(score.getSubmissionStatusCode()));
        }
        report.setAssignmentScores(assignmentScores);
        return report;
    }

    // ---------- helpers ----------

    private Long requireProfessorLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsProfessorStudentMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    /** 통계 카드 3개 (목록 데이터에서 집계) */
    private LmsStudentStatsSummaryDto buildSummary(List<LmsStudentRowDto> students, int totalAssignments) {
        int total = students.size();
        int averageAttendanceRate = total == 0 ? 0
                : (int) Math.round(students.stream()
                        .mapToInt(LmsStudentRowDto::getAttendanceRate)
                        .average()
                        .orElse(0));
        int averageSubmissionRate = 0;
        if (total > 0 && totalAssignments > 0) {
            int submittedSum = students.stream()
                    .mapToInt(s -> s.getSubmittedCount() == null ? 0 : s.getSubmittedCount())
                    .sum();
            averageSubmissionRate = (int) Math.round(submittedSum * 100.0 / ((long) total * totalAssignments));
        }
        return LmsStudentStatsSummaryDto.builder()
                .totalStudents(total)
                .averageAttendanceRate(averageAttendanceRate)
                .averageSubmissionRate(averageSubmissionRate)
                .build();
    }

    /** 출석률(%) = 출석 / 전체 기록 (0 division 보호) */
    private int percent(Integer part, Integer total) {
        if (part == null || total == null || total == 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
