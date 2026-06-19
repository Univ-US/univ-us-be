package com.univus.app.lms.service;

import com.univus.app.lms.dto.LmsStuCalendarDto;
import com.univus.app.lms.mapper.LmsStuCalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SLM-010 학생 캘린더 ServiceImpl (학생 전용). 수강 강의의 주간 강의 시간표를 from~to(학기 경계 적용)로
 * 날짜 전개 + 과제 마감을 합쳐 반환. 표시 규칙(FE 확정): 강의=과목명+분반+시작~종료 / 과제=과제명+마감시각(분반·종료시각 null).
 * 검증 실패는 ResponseStatusException(400/403) — 전역 핸들러가 상태 그대로 매핑(ORA/NPE 500 회피).
 * (교수 캘린더 LmsProfCalendarServiceImpl와 날짜 전개 로직 동일 — '내 강의' 술어만 수강 EXISTS로 다름, 소유경계 분리로 자족 복제.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LmsStuCalendarServiceImpl implements LmsStuCalendarService {

    private final LmsStuCalendarMapper lmsStuCalendarMapper;

    // from/to 형식 가드 ("YYYY-MM-DD") — 틀리면 TO_DATE ORA→500 대신 400
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    // 과도한 날짜 전개 방지 (캘린더는 월 단위 조회 — 1년이면 충분)
    private static final long MAX_RANGE_DAYS = 366;

    private static final String TYPE_LECTURE = "LECTURE";
    private static final String TYPE_ASSIGNMENT = "ASSIGNMENT";

    // LECTURE_TIME.LEC_TIM_DAY_CODE (MON~SUN) → java.time 요일
    private static final Map<String, DayOfWeek> DAY_OF_WEEK = Map.of(
            "MON", DayOfWeek.MONDAY,
            "TUE", DayOfWeek.TUESDAY,
            "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY,
            "FRI", DayOfWeek.FRIDAY,
            "SAT", DayOfWeek.SATURDAY,
            "SUN", DayOfWeek.SUNDAY);

    @Override
    @Transactional(readOnly = true)
    public List<LmsStuCalendarDto.EventResDto> getCalendar(Long memberId, String from, String to) {
        LocalDate fromDate = parseUserDate(from, "from");
        LocalDate toDate = parseUserDate(to, "to");
        if (toDate.isBefore(fromDate)) {
            return Collections.emptyList(); // 빈/역전 범위
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 범위가 너무 넓습니다.");
        }
        Long lmsPrfId = requireStudentLmsPrfId(memberId);

        List<LmsStuCalendarDto.EventResDto> events = new ArrayList<>();
        // 1) 강의 일정 — 수강 강의 주간 시간표를 from~to(학기 경계 적용)로 날짜 전개 (과목명+분반+시작~종료)
        for (LmsStuCalendarDto.LectureTimeRow slot : lmsStuCalendarMapper.selectLectureTimes(lmsPrfId, from, to)) {
            expandLecture(slot, fromDate, toDate, events);
        }
        // 2) 과제 마감 — 범위 내 마감일/시각 (과제명 + 마감시각, 분반·종료시각 없음)
        for (LmsStuCalendarDto.AssignmentDueRow due : lmsStuCalendarMapper.selectAssignmentDues(lmsPrfId, from, to)) {
            events.add(LmsStuCalendarDto.EventResDto.builder()
                    .date(due.getLecAsnDueDate())
                    .type(TYPE_ASSIGNMENT)
                    .title(due.getLecAsnTitle())
                    .time(due.getLecAsnDueTime())
                    .endTime(null)
                    .lecId(due.getLecId())
                    .lecSection(null)
                    .build());
        }
        log.info("학생 캘린더 조회 lmsPrfId={} from={} to={} events={}", lmsPrfId, from, to, events.size());
        return events;
    }

    /* 주간 슬롯을 [from,to] ∩ [학기시작,학기종료] 안에서 해당 요일 날짜로 전개 */
    private void expandLecture(LmsStuCalendarDto.LectureTimeRow slot, LocalDate from, LocalDate to,
                               List<LmsStuCalendarDto.EventResDto> out) {
        DayOfWeek dow = DAY_OF_WEEK.get(slot.getLecTimDayCode());
        if (dow == null) {
            return; // 알 수 없는 요일코드(데이터 이상)는 스킵
        }
        LocalDate start = laterOf(from, LocalDate.parse(slot.getSemStrDate()));
        LocalDate end = earlierOf(to, LocalDate.parse(slot.getSemEndDate()));
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == dow) {
                out.add(LmsStuCalendarDto.EventResDto.builder()
                        .date(d.toString()) // ISO "YYYY-MM-DD"
                        .type(TYPE_LECTURE)
                        .title(slot.getCourseName())
                        .time(slot.getLecTimStrTime())
                        .endTime(slot.getLecTimEndTime())
                        .lecId(slot.getLecId())
                        .lecSection(slot.getLecSection())
                        .build());
            }
        }
    }

    /* 학생 LMS 프로필 확인 — 없으면 403 (PLM-003/004/005/006과 동일 패턴) */
    private Long requireStudentLmsPrfId(Long memberId) {
        Long lmsPrfId = lmsStuCalendarMapper.findLmsPrfIdByMemberId(memberId);
        if (lmsPrfId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학생 LMS 프로필이 없습니다.");
        }
        return lmsPrfId;
    }

    /* from/to 형식 검증 ("YYYY-MM-DD") — 틀리면 400 */
    private LocalDate parseUserDate(String value, String field) {
        if (value == null || !DATE_PATTERN.matcher(value).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 날짜 형식이 올바르지 않습니다.");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 날짜가 올바르지 않습니다.");
        }
    }

    private static LocalDate laterOf(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate earlierOf(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
