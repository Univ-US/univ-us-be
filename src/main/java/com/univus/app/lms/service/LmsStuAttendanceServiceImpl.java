package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuAttendanceDto;
import com.univus.app.lms.mapper.LmsStuAttendanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** SLM-005 학생 출석 내역 ServiceImpl. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsStuAttendanceServiceImpl implements LmsStuAttendanceService {

    private static final String STATUS_LATE = "LAT";
    private static final String STATUS_ABSENT = "ABS";

    private final LmsStuAttendanceMapper lmsStuAttendanceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuAttendanceDto.SemesterAttendanceResDto> getAttendance(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        List<LmsStuAttendanceDto.CourseRow> courseRows = lmsStuAttendanceMapper.selectCourseRows(lmsPrfId);
        Map<Long, List<LmsStuAttendanceDto.RecordRow>> recordsByEnrollment =
                lmsStuAttendanceMapper.selectDetailRecords(lmsPrfId).stream()
                        .collect(Collectors.groupingBy(LmsStuAttendanceDto.RecordRow::getEnrollmentId));

        Map<String, SemesterGroup> groups = new LinkedHashMap<>();
        for (LmsStuAttendanceDto.CourseRow row : courseRows) {
            String key = row.getYear() + "-" + row.getTermCode();
            SemesterGroup group = groups.computeIfAbsent(key, ignored -> new SemesterGroup(row));
            group.courses.add(toCourse(row, recordsByEnrollment.getOrDefault(row.getEnrollmentId(), List.of())));
        }

        List<LmsStuAttendanceDto.SemesterAttendanceResDto> response = new ArrayList<>(groups.size());
        for (SemesterGroup group : groups.values()) {
            response.add(LmsStuAttendanceDto.SemesterAttendanceResDto.builder()
                    .year(group.year)
                    .termCode(group.termCode)
                    .semesterLabel(semesterLabel(group.year, group.termCode))
                    .inProgress(group.inProgress)
                    .courseCount(group.courses.size())
                    .courses(group.courses)
                    .build());
        }
        log.info("학생 출석 내역 조회 lmsPrfId={} semesters={} courses={}", lmsPrfId, response.size(), courseRows.size());
        return response;
    }

    private LmsStuAttendanceDto.AttendanceCourseResDto toCourse(
            LmsStuAttendanceDto.CourseRow row,
            List<LmsStuAttendanceDto.RecordRow> records) {
        List<LmsStuAttendanceDto.AttendanceRecordResDto> lateRecords = records.stream()
                .filter(r -> STATUS_LATE.equals(r.getStatusCode()))
                .map(this::toRecord)
                .toList();
        List<LmsStuAttendanceDto.AttendanceRecordResDto> absentRecords = records.stream()
                .filter(r -> STATUS_ABSENT.equals(r.getStatusCode()))
                .map(this::toRecord)
                .toList();

        return LmsStuAttendanceDto.AttendanceCourseResDto.builder()
                .lecId(row.getLecId())
                .courseName(row.getCourseName())
                .lecSection(safe(row.getLecSection()))
                .totalSessions(safe(row.getTotalSessions()))
                .present(safe(row.getPresent()))
                .late(safe(row.getLate()))
                .absent(safe(row.getAbsent()))
                .attendanceRate(percent(row.getPresent(), row.getAttendanceTotal()))
                .lateRecords(lateRecords)
                .absentRecords(absentRecords)
                .build();
    }

    private LmsStuAttendanceDto.AttendanceRecordResDto toRecord(LmsStuAttendanceDto.RecordRow row) {
        return LmsStuAttendanceDto.AttendanceRecordResDto.builder()
                .date(row.getAttendanceDate())
                .build();
    }

    private Long requireStudentLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsStuAttendanceMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    private int percent(Integer part, Integer total) {
        if (part == null || total == null || total == 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String semesterLabel(Integer year, String termCode) {
        if (termCode == null) {
            return year + "년";
        }
        String termLabel = switch (termCode) {
            case "SM1" -> "1학기";
            case "SMR" -> "여름 계절";
            case "SM2" -> "2학기";
            case "WNT" -> "겨울 계절";
            default -> termCode;
        };
        return year + "년 " + termLabel;
    }

    private static class SemesterGroup {
        private final Integer year;
        private final String termCode;
        private final Boolean inProgress;
        private final List<LmsStuAttendanceDto.AttendanceCourseResDto> courses = new ArrayList<>();

        private SemesterGroup(LmsStuAttendanceDto.CourseRow row) {
            this.year = row.getYear();
            this.termCode = row.getTermCode();
            this.inProgress = row.getInProgressFlag() != null && row.getInProgressFlag() == 1;
        }
    }
}
