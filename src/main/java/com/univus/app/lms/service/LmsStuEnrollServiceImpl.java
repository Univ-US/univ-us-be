package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuEnrollDto;
import com.univus.app.lms.mapper.LmsStuEnrollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LmsStuEnrollServiceImpl implements LmsStuEnrollService {

    /** 신청 가능 학점 한도 (전체 학생 공통 고정값) */
    private static final int MAX_CREDIT = 21;

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

    private final LmsStuEnrollMapper lmsStuEnrollMapper;

    @Override
    @Transactional(readOnly = true)
    public LmsStuEnrollDto.SummaryResDto getSummary(Long memberId) {
        requireStudentLmsPrfId(memberId);
        Long univId = lmsStuEnrollMapper.findUnivIdByMemberId(memberId);
        LmsStuEnrollDto.TargetSemesterRow semester = lmsStuEnrollMapper.selectTargetSemester(univId);
        return LmsStuEnrollDto.SummaryResDto.builder()
                .semesterLabel(semester == null ? null : semesterLabel(semester.getSemYear(), semester.getSemTerm()))
                .maxCredit(MAX_CREDIT)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuEnrollDto.LectureRow> getOpenLectures(Long memberId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        Long univId = lmsStuEnrollMapper.findUnivIdByMemberId(memberId);
        LmsStuEnrollDto.TargetSemesterRow semester = lmsStuEnrollMapper.selectTargetSemester(univId);
        if (semester == null) {
            return List.of();
        }
        List<LmsStuEnrollDto.LectureFlatRow> rows =
                lmsStuEnrollMapper.selectOpenLecturesFlat(semester.getSemId(), lmsPrfId, univId);
        return assembleLectureRows(rows);
    }

    @Override
    @Transactional
    public LmsStuEnrollDto.SubmitResultDto submitEnrollment(Long memberId, List<Long> lecIds) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        Long univId = lmsStuEnrollMapper.findUnivIdByMemberId(memberId);
        List<Long> success = new ArrayList<>();
        List<LmsStuEnrollDto.FailedItem> failed = new ArrayList<>();
        if (lecIds == null || lecIds.isEmpty()) {
            return LmsStuEnrollDto.SubmitResultDto.builder().success(success).failed(failed).build();
        }

        Long targetSemId = null;
        int accumulatedCredit = 0;
        List<LmsStuEnrollDto.TimeSlotRow> accumulatedSchedule = new ArrayList<>();

        // 같은 배치 안에서 여러 강좌를 잠그는 순서를 모든 요청에서 동일하게(오름차순) 고정 — 두 배치 신청이
        // 서로 다른 순서로 같은 강좌 2건을 잠그면서 데드락이 나는 걸 방지
        List<Long> sortedLecIds = new ArrayList<>(lecIds);
        sortedLecIds.sort(Comparator.naturalOrder());

        for (Long lecId : sortedLecIds) {
            LmsStuEnrollDto.LectureForEnrollRow lecture = lmsStuEnrollMapper.selectLectureForEnroll(lecId);
            if (lecture == null || !univId.equals(lecture.getUnivId())) {
                failed.add(fail(lecId, "존재하지 않는 강좌입니다."));
                continue;
            }
            if (!"OPEN".equals(lecture.getLecValStatus())) {
                failed.add(fail(lecId, "수강신청 기간이 아닌 강좌입니다."));
                continue;
            }
            if (targetSemId == null) {
                targetSemId = lecture.getSemId();
                accumulatedCredit = lmsStuEnrollMapper.sumActiveCredits(lmsPrfId, targetSemId);
                accumulatedSchedule = new ArrayList<>(
                        lmsStuEnrollMapper.selectActiveScheduleRows(lmsPrfId, targetSemId));
            }

            LmsStuEnrollDto.EnrollmentRow existing = lmsStuEnrollMapper.findEnrollmentRow(lmsPrfId, lecId);
            if (existing != null && !"DRP".equals(existing.getStatus())) {
                failed.add(fail(lecId, "이미 신청한 강좌입니다."));
                continue;
            }

            // 정원 동시성 제어: 강좌 row를 잠궈 같은 강좌에 대한 동시 신청을 직렬화한 뒤 최신 인원으로 재조회
            // (잠그기 전에 읽은 enrolledCount는 동시 요청 중인 다른 트랜잭션 탓에 이미 stale일 수 있음)
            lmsStuEnrollMapper.lockLectureForEnroll(lecId);
            lecture = lmsStuEnrollMapper.selectLectureForEnroll(lecId);

            int capacity = lecture.getCapacity() == null ? -1 : lecture.getCapacity();
            int enrolledCount = lecture.getEnrolledCount() == null ? 0 : lecture.getEnrolledCount();
            if (capacity >= 0 && enrolledCount >= capacity) {
                failed.add(fail(lecId, "정원이 초과되었습니다."));
                continue;
            }

            List<LmsStuEnrollDto.TimeSlotRow> candidateSlots = lmsStuEnrollMapper.selectLectureTimeSlots(lecId);
            if (hasConflict(candidateSlots, accumulatedSchedule)) {
                failed.add(fail(lecId, "시간표가 겹칩니다."));
                continue;
            }

            int credit = lecture.getLecCredit() == null ? 0 : lecture.getLecCredit();
            if (accumulatedCredit + credit > MAX_CREDIT) {
                failed.add(fail(lecId, "신청 가능 학점을 초과합니다."));
                continue;
            }

            // INSERT/UPDATE 자체도 같은 정원 조건을 다시 검증 — 락 보유 중이라 사실상 항상 충족되지만
            // (예: 동시에 관리자가 정원을 줄이는 등의 극단적 케이스) 방어적으로 영향받은 행 수를 확인
            int affected = existing != null
                    ? lmsStuEnrollMapper.reactivateEnrollment(existing.getLecStdEnrId())
                    : lmsStuEnrollMapper.insertEnrollment(lmsPrfId, lecId);
            if (affected == 0) {
                failed.add(fail(lecId, "정원이 초과되었습니다."));
                continue;
            }
            accumulatedCredit += credit;
            accumulatedSchedule.addAll(candidateSlots);
            success.add(lecId);
        }

        return LmsStuEnrollDto.SubmitResultDto.builder().success(success).failed(failed).build();
    }

    @Override
    @Transactional
    public void cancelEnrollment(Long memberId, Long lecId) {
        Long lmsPrfId = requireStudentLmsPrfId(memberId);
        int affected = lmsStuEnrollMapper.cancelEnrollment(lmsPrfId, lecId);
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다.");
        }
    }

    private LmsStuEnrollDto.FailedItem fail(Long lecId, String reason) {
        return LmsStuEnrollDto.FailedItem.builder().lecId(lecId).reason(reason).build();
    }

    /** 같은 요일에서 시작-종료 시간이 겹치는지 검사 (문자열 "HH:MI", 사전식 비교로 시각 비교 가능) */
    private boolean hasConflict(List<LmsStuEnrollDto.TimeSlotRow> candidate, List<LmsStuEnrollDto.TimeSlotRow> existing) {
        for (LmsStuEnrollDto.TimeSlotRow a : candidate) {
            for (LmsStuEnrollDto.TimeSlotRow b : existing) {
                if (a.getDayCode().equals(b.getDayCode())
                        && a.getStartTime().compareTo(b.getEndTime()) < 0
                        && b.getStartTime().compareTo(a.getEndTime()) < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<LmsStuEnrollDto.LectureRow> assembleLectureRows(List<LmsStuEnrollDto.LectureFlatRow> rows) {
        Map<Long, LectureAccumulator> lectures = new LinkedHashMap<>();
        for (LmsStuEnrollDto.LectureFlatRow row : rows) {
            lectures.computeIfAbsent(row.getLecId(), key -> new LectureAccumulator(row)).addSchedule(row);
        }
        return lectures.values().stream().map(LectureAccumulator::toResponse).toList();
    }

    private Long requireStudentLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsStuEnrollMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    private static String semesterLabel(Integer year, String termCode) {
        return year + "년 " + TERM_LABELS.getOrDefault(termCode, termCode);
    }

    private static String formatSlot(String dayCode, String startTime, String endTime) {
        if (dayCode == null || startTime == null || endTime == null) {
            return null;
        }
        return DAY_LABELS.getOrDefault(dayCode, dayCode) + " " + startTime + "-" + endTime;
    }

    private static class LectureAccumulator {
        private final Long lecId;
        private final String courseCode;
        private final String courseName;
        private final String professor;
        private final String department;
        private final Integer lecCredit;
        private final Integer lecSection;
        private final Integer capacity;
        private final Integer enrolledCount;
        private final boolean alreadyEnrolled;
        private final List<LmsStuEnrollDto.ScheduleSlot> scheduleSlots = new ArrayList<>();
        private final List<String> scheduleLabels = new ArrayList<>();

        private LectureAccumulator(LmsStuEnrollDto.LectureFlatRow row) {
            this.lecId = row.getLecId();
            this.courseCode = row.getCourseCode();
            this.courseName = row.getCourseName();
            this.professor = row.getProfessor();
            this.department = row.getDepartment();
            this.lecCredit = row.getLecCredit();
            this.lecSection = row.getLecSection();
            this.capacity = row.getCapacity();
            this.enrolledCount = row.getEnrolledCount();
            this.alreadyEnrolled = row.getAlreadyEnrolledFlag() != null && row.getAlreadyEnrolledFlag() == 1;
        }

        private void addSchedule(LmsStuEnrollDto.LectureFlatRow row) {
            if (row.getLecTimDayCode() == null) {
                return;
            }
            scheduleSlots.add(LmsStuEnrollDto.ScheduleSlot.builder()
                    .day(DAY_LABELS.getOrDefault(row.getLecTimDayCode(), row.getLecTimDayCode()))
                    .start(row.getLecTimStrTime())
                    .end(row.getLecTimEndTime())
                    .build());
            String label = formatSlot(row.getLecTimDayCode(), row.getLecTimStrTime(), row.getLecTimEndTime());
            if (label != null) {
                scheduleLabels.add(label);
            }
        }

        private LmsStuEnrollDto.LectureRow toResponse() {
            return LmsStuEnrollDto.LectureRow.builder()
                    .lecId(lecId)
                    .courseCode(courseCode)
                    .courseName(courseName)
                    .professor(professor)
                    .department(department)
                    .lecCredit(lecCredit)
                    .lecSection(lecSection)
                    .schedule(scheduleLabels.isEmpty() ? "-" : String.join(" / ", scheduleLabels))
                    .scheduleSlots(scheduleSlots)
                    .capacity(capacity)
                    .enrolledCount(enrolledCount)
                    .alreadyEnrolled(alreadyEnrolled)
                    .build();
        }
    }
}
