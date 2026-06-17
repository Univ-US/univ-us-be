package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfCoursesDto;
import com.univus.app.lms.mapper.LmsProfCoursesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* PLM-002 교수 강의 내역 ServiceImpl — flat row를 학기별 그룹핑 + 진행중 학기 KPI 집계 */
@Service
@RequiredArgsConstructor
public class LmsProfCoursesServiceImpl implements LmsProfCoursesService {

    private final LmsProfCoursesMapper lmsProfCoursesMapper;

    @Override
    @Transactional(readOnly = true)
    public LmsProfCoursesDto.ResDto getCourses(Long memberId) {
        Long lmsPrfId = lmsProfCoursesMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }

        List<LmsProfCoursesDto.CourseFlatRow> rows = lmsProfCoursesMapper.selectCoursesFlat(lmsPrfId);

        // 학기별 그룹핑 (flat row가 SEM_STR_DATE DESC 정렬 → LinkedHashMap이 최신순 보존)
        Map<Long, LmsProfCoursesDto.SemesterResDto> bySem = new LinkedHashMap<>();
        for (LmsProfCoursesDto.CourseFlatRow r : rows) {
            LmsProfCoursesDto.SemesterResDto sem = bySem.computeIfAbsent(r.getSemId(), k ->
                    LmsProfCoursesDto.SemesterResDto.builder()
                            .semYear(r.getSemYear())
                            .semTerm(r.getSemTerm())
                            .semesterLabel(semesterLabel(r.getSemYear(), r.getSemTerm()))
                            .inProgress(r.getInProgressFlag() == 1)
                            .courses(new ArrayList<>())
                            .build());
            sem.getCourses().add(LmsProfCoursesDto.CourseResDto.builder()
                    .lecId(r.getLecId())
                    .courseName(r.getCourseName())
                    .lecSection(r.getLecSection() == null ? 0 : r.getLecSection())
                    .studentCount(r.getStudentCount())
                    .schedule(r.getSchedule() == null ? "" : r.getSchedule())
                    .attendanceRate(r.getAttendanceRate() == null ? 0 : r.getAttendanceRate())
                    .ungradedCount(r.getUngradedCount())
                    .build());
        }

        // 학기별 집계 (courseCount/studentTotal — 화면 미사용이나 FE 타입 계약 유지)
        List<LmsProfCoursesDto.SemesterResDto> semesters = new ArrayList<>(bySem.values());
        for (LmsProfCoursesDto.SemesterResDto sem : semesters) {
            sem.setCourseCount(sem.getCourses().size());
            sem.setStudentTotal(sem.getCourses().stream()
                    .mapToInt(LmsProfCoursesDto.CourseResDto::getStudentCount).sum());
        }

        return LmsProfCoursesDto.ResDto.builder()
                .overview(buildOverview(semesters))
                .semesters(semesters)
                .build();
    }

    /** KPI = 진행중 학기 1개 기준 (없으면 가장 최근 시작 학기 = 첫 그룹). 집계: 총 수강생=연인원 합·평균 출석률=강의 단순평균 */
    private LmsProfCoursesDto.OverviewResDto buildOverview(List<LmsProfCoursesDto.SemesterResDto> semesters) {
        LmsProfCoursesDto.SemesterResDto current = semesters.stream()
                .filter(LmsProfCoursesDto.SemesterResDto::isInProgress)
                .findFirst()
                .orElse(semesters.isEmpty() ? null : semesters.get(0));

        if (current == null) {
            return LmsProfCoursesDto.OverviewResDto.builder()
                    .courseCount(0).studentTotal(0).ungradedTotal(0).avgAttendanceRate(0).build();
        }

        List<LmsProfCoursesDto.CourseResDto> courses = current.getCourses();
        int courseCount = courses.size();
        int studentTotal = courses.stream().mapToInt(LmsProfCoursesDto.CourseResDto::getStudentCount).sum();
        int ungradedTotal = courses.stream().mapToInt(LmsProfCoursesDto.CourseResDto::getUngradedCount).sum();
        int avgAttendanceRate = courseCount == 0 ? 0
                : (int) Math.round(courses.stream()
                        .mapToInt(LmsProfCoursesDto.CourseResDto::getAttendanceRate)
                        .average().orElse(0));

        return LmsProfCoursesDto.OverviewResDto.builder()
                .courseCount(courseCount)
                .studentTotal(studentTotal)
                .ungradedTotal(ungradedTotal)
                .avgAttendanceRate(avgAttendanceRate)
                .build();
    }

    /** "2026년 1학기" 라벨 조립 (SEM_TERM 4종 고정) */
    private String semesterLabel(Integer year, String termCode) {
        return year + "년 " + termLabel(termCode);
    }

    private String termLabel(String termCode) {
        if (termCode == null) {
            return "";
        }
        switch (termCode) {
            case "SM1": return "1학기";
            case "SM2": return "2학기";
            case "SMR": return "여름 계절";
            case "WNT": return "겨울 계절";
            default: return termCode;
        }
    }
}
