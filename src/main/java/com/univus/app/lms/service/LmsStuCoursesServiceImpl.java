package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuCoursesDto;
import com.univus.app.lms.mapper.LmsStuCoursesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LmsStuCoursesServiceImpl implements LmsStuCoursesService {

    private static final Map<String, String> TERM_LABELS = Map.of(
            "SM1", "1학기",
            "SMR", "여름 계절",
            "SM2", "2학기",
            "WNT", "겨울 계절");

    private static final Map<String, String> DAY_LABELS = Map.of(
            "MON", "월",
            "TUE", "화",
            "WED", "수",
            "THU", "목",
            "FRI", "금",
            "SAT", "토",
            "SUN", "일");

    private final LmsStuCoursesMapper lmsStuCoursesMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuCoursesDto.SemesterCoursesResDto> getCourses(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        List<LmsStuCoursesDto.CourseFlatRow> rows = lmsStuCoursesMapper.selectStudentCourseRows(lmsPrfId);

        Map<String, SemesterAccumulator> semesters = new LinkedHashMap<>();
        for (LmsStuCoursesDto.CourseFlatRow row : rows) {
            String semesterKey = row.getYear() + ":" + row.getTermCode();
            SemesterAccumulator semester = semesters.computeIfAbsent(
                    semesterKey,
                    key -> new SemesterAccumulator(row.getYear(), row.getTermCode(), isInProgress(row)));
            semester.add(row);
        }

        return semesters.values().stream()
                .map(SemesterAccumulator::toResponse)
                .toList();
    }

    private Long requireStudentLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsStuCoursesMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    private boolean isInProgress(LmsStuCoursesDto.CourseFlatRow row) {
        return row.getInProgressFlag() != null && row.getInProgressFlag() == 1;
    }

    private static String semesterLabel(Integer year, String termCode) {
        return year + "년 " + TERM_LABELS.getOrDefault(termCode, termCode);
    }

    private static String formatSchedule(String dayCode, String startTime, String endTime) {
        if (dayCode == null || startTime == null || endTime == null) {
            return null;
        }
        return DAY_LABELS.getOrDefault(dayCode, dayCode) + " " + startTime + "~" + endTime;
    }

    private static class SemesterAccumulator {
        private final Integer year;
        private final String termCode;
        private final boolean inProgress;
        private final Map<Long, CourseAccumulator> courses = new LinkedHashMap<>();

        private SemesterAccumulator(Integer year, String termCode, boolean inProgress) {
            this.year = year;
            this.termCode = termCode;
            this.inProgress = inProgress;
        }

        private void add(LmsStuCoursesDto.CourseFlatRow row) {
            CourseAccumulator course = courses.computeIfAbsent(row.getLecId(), key -> new CourseAccumulator(row));
            course.addSchedule(row);
        }

        private LmsStuCoursesDto.SemesterCoursesResDto toResponse() {
            List<LmsStuCoursesDto.CourseRow> courseRows = courses.values().stream()
                    .map(CourseAccumulator::toResponse)
                    .toList();
            int totalCredits = courses.values().stream()
                    .mapToInt(CourseAccumulator::credit)
                    .sum();

            return LmsStuCoursesDto.SemesterCoursesResDto.builder()
                    .year(year)
                    .termCode(termCode)
                    .semesterLabel(semesterLabel(year, termCode))
                    .inProgress(inProgress)
                    .courseCount(courseRows.size())
                    .totalCredits(totalCredits)
                    .courses(courseRows)
                    .build();
        }
    }

    private static class CourseAccumulator {
        private final Long lecId;
        private final String courseName;
        private final Integer lecSection;
        private final Integer credit;
        private final String professor;
        private final List<String> schedules = new ArrayList<>();

        private CourseAccumulator(LmsStuCoursesDto.CourseFlatRow row) {
            this.lecId = row.getLecId();
            this.courseName = row.getCourseName();
            this.lecSection = row.getLecSection();
            this.credit = row.getCredit() == null ? 0 : row.getCredit();
            this.professor = row.getProfessor();
        }

        private void addSchedule(LmsStuCoursesDto.CourseFlatRow row) {
            String schedule = formatSchedule(row.getDayCode(), row.getStartTime(), row.getEndTime());
            if (schedule != null && !schedules.contains(schedule)) {
                schedules.add(schedule);
            }
        }

        private int credit() {
            return credit;
        }

        private LmsStuCoursesDto.CourseRow toResponse() {
            return LmsStuCoursesDto.CourseRow.builder()
                    .lecId(lecId)
                    .courseName(courseName)
                    .lecSection(lecSection)
                    .credit(credit)
                    .professor(professor)
                    .schedule(schedules.isEmpty() ? "-" : String.join(" · ", schedules))
                    .build();
        }
    }
}
