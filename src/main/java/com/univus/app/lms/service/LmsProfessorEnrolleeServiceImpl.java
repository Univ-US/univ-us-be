package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsAssignmentScoreDto;
import com.univus.app.lms.dto.LmsLectureDto;
import com.univus.app.lms.dto.LmsLectureStudentsResponseDto;
import com.univus.app.lms.dto.LmsPageInfoDto;
import com.univus.app.lms.dto.LmsSemesterResponseDto;
import com.univus.app.lms.dto.LmsStudentReportDto;
import com.univus.app.lms.dto.LmsStudentRowDto;
import com.univus.app.lms.dto.LmsStudentStatsSummaryDto;
import com.univus.app.lms.mapper.LmsProfessorStudentMapper;
import com.univus.app.lms.support.LmsEnrolleeExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/* PLM-003 교수 페이지 - 수강생 현황 ServiceImpl */
@Service
@RequiredArgsConstructor
public class LmsProfessorEnrolleeServiceImpl implements LmsProfessorEnrolleeService {

    /** 미제출 코드 (공통코드 LEC_ASN_SBM_STATUS = NSB). 코드→라벨은 FE가 공통코드 API로 매핑 */
    private static final String NOT_SUBMITTED_CODE = "NSB";
    /** 수강생 목록 기본 페이지 크기 (10명 초과 시 페이지네이션) */
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final LmsProfessorStudentMapper lmsProfessorStudentMapper;
    private final LmsEnrolleeExcelExporter enrolleeExcelExporter;

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

    /** PLM-003 : 강의별 수강생 목록(현재 페이지) + 통계 카드 3개 (검색·제출필터·정렬·페이지네이션) */
    @Override
    @Transactional(readOnly = true)
    public LmsLectureStudentsResponseDto getLectureStudents(Long professorMemberId, Long lecId,
                                                            String search, String submission,
                                                            String sort, String order,
                                                            int page, int size) {
        LmsLectureStudentsResponseDto data =
                buildFilteredStudents(professorMemberId, lecId, search, submission, sort, order);

        // 필터·정렬된 전체 목록을 페이지로 슬라이스 (통계 카드는 전체 기준 유지)
        List<LmsStudentRowDto> all = data.getStudents();
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        int safePage = Math.max(page, 0);
        int totalElements = all.size();
        // 수강생이 0~size명이어도 페이지는 최소 1페이지로 명시 (totalPages ≥ 1)
        int totalPages = Math.max(1, (totalElements + safeSize - 1) / safeSize);
        int fromIndex = (int) Math.min((long) safePage * safeSize, totalElements); // 큰 page 오버플로 방지
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        data.setStudents(new ArrayList<>(all.subList(fromIndex, toIndex)));
        data.setPagination(LmsPageInfoDto.builder()
                .page(safePage)
                .size(safeSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build());
        return data;
    }

    /** PLM-003 : 수강생 명단 Excel(.xlsx) 내보내기 — 검색·필터·정렬 결과 전체(페이지네이션 무시) */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportLectureStudentsExcel(Long professorMemberId, Long lecId,
                                             String search, String submission,
                                             String sort, String order) {
        // 페이지 슬라이스 없이 전체 목록을 내보냄 (소유권 검증도 내부에서 함께 수행)
        LmsLectureStudentsResponseDto data =
                buildFilteredStudents(professorMemberId, lecId, search, submission, sort, order);
        return enrolleeExcelExporter.toExcel(data);
    }

    /** 공통: 강의 헤더 + 통계 카드 + (검색·제출필터·정렬 적용된) 전체 목록 — 페이지네이션 직전 단계 */
    private LmsLectureStudentsResponseDto buildFilteredStudents(Long professorMemberId, Long lecId,
                                                                String search, String submission,
                                                                String sort, String order) {
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

        // 통계 카드는 검색된 전체 수강생 기준 — 제출 필터·정렬·페이지와 무관
        LmsStudentStatsSummaryDto summary = buildSummary(students, totalAssignments);
        // 목록에 제출 필터 + 정렬 적용 (서버사이드)
        List<LmsStudentRowDto> view =
                applyFilterAndSort(students, blankToNull(submission), blankToNull(sort), order);

        return LmsLectureStudentsResponseDto.builder()
                .lecture(lecture)
                .summary(summary)
                .students(view)
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

    /** 목록에 제출 필터 + 정렬 적용 (통계 카드는 영향받지 않음) */
    private List<LmsStudentRowDto> applyFilterAndSort(List<LmsStudentRowDto> students,
                                                      String submission, String sort, String order) {
        List<LmsStudentRowDto> view = students;
        if (submission != null) {
            view = view.stream().filter(s -> matchesSubmission(s, submission)).toList();
        }
        Comparator<LmsStudentRowDto> comparator = resolveComparator(sort, order);
        if (comparator != null) {
            view = view.stream().sorted(comparator).toList();
        }
        return view;
    }

    /** 과제 제출 필터: complete=전부 제출(제출수≥전체) / incomplete=미제출 존재 */
    private boolean matchesSubmission(LmsStudentRowDto student, String submission) {
        int submitted = student.getSubmittedCount() == null ? 0 : student.getSubmittedCount();
        int total = student.getTotalAssignments() == null ? 0 : student.getTotalAssignments();
        if ("complete".equalsIgnoreCase(submission)) {
            return submitted >= total;
        }
        if ("incomplete".equalsIgnoreCase(submission)) {
            return submitted < total;
        }
        return true; // 알 수 없는 값은 필터 미적용
    }

    /** 정렬: sort=name|studentNo|attendance|score, order=asc|desc(기본 asc). 미채점 평균점수(null)는 방향 무관 항상 뒤 */
    private Comparator<LmsStudentRowDto> resolveComparator(String sort, String order) {
        if (sort == null) {
            return null; // 정렬 미지정 → 기본 이름순(매퍼 ORDER BY) 유지
        }
        boolean desc = "desc".equalsIgnoreCase(order);
        if ("name".equalsIgnoreCase(sort)) {
            Comparator<String> direction = desc ? Comparator.reverseOrder() : Comparator.naturalOrder();
            return Comparator.comparing(LmsStudentRowDto::getStudentName, Comparator.nullsLast(direction));
        }
        if ("studentNo".equalsIgnoreCase(sort)) {
            Comparator<String> direction = desc ? Comparator.reverseOrder() : Comparator.naturalOrder();
            return Comparator.comparing(LmsStudentRowDto::getStudentNo, Comparator.nullsLast(direction));
        }
        if ("attendance".equalsIgnoreCase(sort)) {
            Comparator<LmsStudentRowDto> base = Comparator.comparingInt(LmsStudentRowDto::getAttendanceRate);
            return desc ? base.reversed() : base;
        }
        if ("score".equalsIgnoreCase(sort)) {
            Comparator<Integer> direction = desc ? Comparator.reverseOrder() : Comparator.naturalOrder();
            return Comparator.comparing(LmsStudentRowDto::getAverageScore, Comparator.nullsLast(direction));
        }
        return null; // 알 수 없는 정렬 키 → 기본 이름순 유지
    }
}
