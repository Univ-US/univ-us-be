package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminOperationLogDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceAdminOperationLogServiceImpl implements ServiceAdminOperationLogService {

    private static final int PAGE_SIZE = 12;
    private static final Set<String> CATEGORIES = Set.of("ACCESS", "PAYMENT");
    private static final Set<String> RESULTS = Set.of("SUCCESS", "FAILURE", "SCHEDULED");
    private static final Set<Integer> PERIODS = Set.of(1, 7, 30, 90);

    private final ServiceAdminOperationLogMapper serviceAdminOperationLogMapper;

    @Transactional(readOnly = true)
    @Override
    public ServiceAdminOperationLogDto.Page getOperationLogs(
            int page,
            String keyword,
            String category,
            String result,
            String period
    ) {
        int safePage = Math.max(page, 0);
        ServiceAdminOperationLogDto.Search search =
                new ServiceAdminOperationLogDto.Search();
        search.setKeyword(normalizeKeyword(keyword));
        search.setCategory(normalizeOption(category, CATEGORIES, "로그 종류"));
        search.setResult(normalizeOption(result, RESULTS, "결과"));
        search.setPeriodDays(normalizePeriod(period));
        search.setOffset(safePage * PAGE_SIZE);

        return new ServiceAdminOperationLogDto.Page(
                serviceAdminOperationLogMapper.selectOperationLogs(search),
                safePage,
                PAGE_SIZE,
                serviceAdminOperationLogMapper.countOperationLogs(search),
                serviceAdminOperationLogMapper.selectOperationLogSummary(search)
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeOption(
            String value,
            Set<String> allowed,
            String label
    ) {
        if (value == null || value.trim().isEmpty()
                || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 " + label + "입니다.");
        }
        return normalized;
    }

    private Integer normalizePeriod(String period) {
        if (period == null || period.trim().isEmpty()
                || "ALL".equalsIgnoreCase(period.trim())) {
            return null;
        }
        int days;
        try {
            days = Integer.parseInt(period.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("지원하지 않는 기간 필터입니다.");
        }
        if (!PERIODS.contains(days)) {
            throw new IllegalArgumentException("지원하지 않는 기간 필터입니다.");
        }
        return days;
    }
}
