package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsProfAttendanceDto;
import com.univus.app.lms.mapper.LmsProfAttendanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* PLM-008 교수 출결 관리 ServiceImpl */
@Service
@RequiredArgsConstructor
public class LmsProfAttendanceServiceImpl implements LmsProfAttendanceService {

    /** 이 화면이 다루는 출결 상태 3종 (ATTD_STS 중 ELV·EXC는 VAL_STATUS DEL 미사용) */
    private static final Set<String> ALLOWED_STATUS = Set.of("PRS", "LAT", "ABS");

    private final LmsProfAttendanceMapper lmsProfAttendanceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LmsProfAttendanceDto.LectureResDto> getLectures(Long professorMemberId) {
        Long lmsPrfId = lmsProfAttendanceMapper.findLmsPrfIdByMemberId(professorMemberId);
        if (lmsPrfId == null) {
            return List.of();
        }
        return lmsProfAttendanceMapper.selectLectures(lmsPrfId);
    }

    @Override
    @Transactional(readOnly = true)
    public LmsProfAttendanceDto.LectureAttendanceResDto getLectureAttendance(Long professorMemberId, Long lecId) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(professorMemberId);

        LmsProfAttendanceDto.LectureResDto lecture =
                lmsProfAttendanceMapper.selectLectureDetail(lecId, professorLmsPrfId);
        if (lecture == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 담당하는 강의가 아닙니다.");
        }

        List<LmsProfAttendanceDto.StudentRow> studentRows =
                lmsProfAttendanceMapper.selectStudents(lecId, professorLmsPrfId);
        List<LmsProfAttendanceDto.SessionRow> sessionRows =
                lmsProfAttendanceMapper.selectSessions(lecId, professorLmsPrfId);

        // 회차를 수강신청별로 그룹핑 (selectSessions는 enrollmentId·날짜순 정렬)
        Map<Long, List<LmsProfAttendanceDto.SessionRow>> byEnrollment = new LinkedHashMap<>();
        for (LmsProfAttendanceDto.SessionRow sr : sessionRows) {
            byEnrollment.computeIfAbsent(sr.getEnrollmentId(), k -> new ArrayList<>()).add(sr);
        }

        List<LmsProfAttendanceDto.StudentResDto> students = new ArrayList<>(studentRows.size());
        for (LmsProfAttendanceDto.StudentRow row : studentRows) {
            students.add(buildStudent(row, byEnrollment.getOrDefault(row.getEnrollmentId(), List.of())));
        }

        return LmsProfAttendanceDto.LectureAttendanceResDto.builder()
                .lecture(lecture)
                .summary(summarize(students))
                .students(students)
                .build();
    }

    @Override
    @Transactional
    public LmsProfAttendanceDto.StudentResDto updateStudentAttendance(Long professorMemberId, Long lecId,
                                                                      Long studentMemberId,
                                                                      List<LmsProfAttendanceDto.SessionUpdateDto> sessions) {
        Long professorLmsPrfId = requireProfessorLmsPrfId(professorMemberId);

        Long enrollmentId = lmsProfAttendanceMapper.findEnrollmentId(lecId, professorLmsPrfId, studentMemberId);
        if (enrollmentId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 강의의 수강생을 찾을 수 없습니다.");
        }

        if (sessions != null) {
            for (LmsProfAttendanceDto.SessionUpdateDto su : sessions) {
                if (su.getSessionId() == null || su.getStdEnrAtdStsCode() == null
                        || !ALLOWED_STATUS.contains(su.getStdEnrAtdStsCode())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "허용되지 않는 출결 상태입니다 (출석/지각/결석만 가능).");
                }
                // enrollmentId 스코프 → 본인 강의·해당 학생 회차만 변경(타 학생/강의 회차는 0행 영향)
                lmsProfAttendanceMapper.updateSessionStatus(su.getSessionId(), enrollmentId, su.getStdEnrAtdStsCode());
            }
        }

        // 갱신된 학생 행 재조회
        LmsProfAttendanceDto.StudentRow row =
                lmsProfAttendanceMapper.selectStudentByMember(lecId, professorLmsPrfId, studentMemberId);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 강의의 수강생을 찾을 수 없습니다.");
        }
        List<LmsProfAttendanceDto.SessionRow> updatedSessions =
                lmsProfAttendanceMapper.selectSessionsByEnrollment(enrollmentId);
        return buildStudent(row, updatedSessions);
    }

    // ---------- helpers ----------

    private Long requireProfessorLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsProfAttendanceMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "교수 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    /** 학생 1명 + 회차들 → 응답 DTO (출석/지각/결석 카운트·출석률 계산) */
    private LmsProfAttendanceDto.StudentResDto buildStudent(LmsProfAttendanceDto.StudentRow row,
                                                            List<LmsProfAttendanceDto.SessionRow> sessionRows) {
        List<LmsProfAttendanceDto.SessionResDto> sessions = new ArrayList<>(sessionRows.size());
        int present = 0;
        int late = 0;
        int absent = 0;
        for (LmsProfAttendanceDto.SessionRow sr : sessionRows) {
            sessions.add(LmsProfAttendanceDto.SessionResDto.builder()
                    .sessionId(sr.getSessionId())
                    .stdEnrAtdRegDate(sr.getStdEnrAtdRegDate())
                    .stdEnrAtdStsCode(sr.getStdEnrAtdStsCode())
                    .build());
            String st = sr.getStdEnrAtdStsCode();
            if ("PRS".equals(st)) {
                present++;
            } else if ("LAT".equals(st)) {
                late++;
            } else if ("ABS".equals(st)) {
                absent++;
            }
        }
        int total = present + late + absent;
        return LmsProfAttendanceDto.StudentResDto.builder()
                .enrollmentId(row.getEnrollmentId())
                .memberId(row.getMemberId())
                .studentName(row.getStudentName())
                .studentNo(row.getStudentNo())
                .imageUrl(row.getImageUrl())
                .totalSessions(total)
                .present(present)
                .late(late)
                .absent(absent)
                .attendanceRate(percent(present, total))
                .sessions(sessions)
                .build();
    }

    /**
     * 통계 카드 — 학생별 출석률·지각률을 평균(반올림), 결석률은 잔여(100−출석−지각)로 보정해 셋 합 100%.
     * (per-student round 후 평균 = PLM-003 buildSummary 동일 규칙 + 출결 3지표 합100 보정)
     */
    private LmsProfAttendanceDto.SummaryResDto summarize(List<LmsProfAttendanceDto.StudentResDto> students) {
        int totalStudents = students.size();
        if (totalStudents == 0) {
            return LmsProfAttendanceDto.SummaryResDto.builder()
                    .totalStudents(0).averageAttendanceRate(0).averageLateRate(0).averageAbsentRate(0)
                    .build();
        }
        int averageAttendanceRate = (int) Math.round(students.stream()
                .mapToInt(LmsProfAttendanceDto.StudentResDto::getAttendanceRate)
                .average().orElse(0));
        int averageLateRate = (int) Math.round(students.stream()
                .mapToInt(s -> percent(s.getLate(), s.getTotalSessions()))
                .average().orElse(0));
        int averageAbsentRate = Math.max(0, 100 - averageAttendanceRate - averageLateRate);
        return LmsProfAttendanceDto.SummaryResDto.builder()
                .totalStudents(totalStudents)
                .averageAttendanceRate(averageAttendanceRate)
                .averageLateRate(averageLateRate)
                .averageAbsentRate(averageAbsentRate)
                .build();
    }

    /** 비율(%) = part / total (0 division 보호) */
    private int percent(int part, int total) {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / total);
    }
}
