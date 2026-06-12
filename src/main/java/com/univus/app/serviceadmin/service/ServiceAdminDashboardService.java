package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.dto.ServiceAdminDashboardDto;
import com.univus.app.serviceadmin.mapper.ServiceAdminDashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAdminDashboardService {

    private static final int CHANGE_RATE_SCALE = 1;

    private final ServiceAdminDashboardMapper serviceAdminDashboardMapper;

    @Transactional(readOnly = true)
    public ServiceAdminDashboardDto.Response getDashboard() {
        ServiceAdminDashboardDto.Summary summary =
                serviceAdminDashboardMapper.selectDashboardSummary();

        if (summary == null) {
            summary = new ServiceAdminDashboardDto.Summary();
        }

        summary.setRevenueChangeRate(calculateChangeRate(
                summary.getCurrentMonthRevenue(),
                summary.getPreviousMonthRevenue()
        ));
        summary.setNewMemberChangeRate(calculateChangeRate(
                summary.getCurrentMonthNewMemberCount(),
                summary.getPreviousMonthNewMemberCount()
        ));

        List<ServiceAdminDashboardDto.SchoolOverview> schools =
                serviceAdminDashboardMapper.selectSchoolOverview();
        List<ServiceAdminDashboardDto.RecentMember> recentMembers =
                serviceAdminDashboardMapper.selectRecentMembers();

        return ServiceAdminDashboardDto.Response.builder()
                .summary(summary)
                .schools(schools)
                .recentMembers(recentMembers)
                .build();
    }

    private BigDecimal calculateChangeRate(Number current, Number previous) {
        BigDecimal currentValue = toBigDecimal(current);
        BigDecimal previousValue = toBigDecimal(previous);

        if (previousValue.signum() == 0) {
            return currentValue.signum() == 0 ? BigDecimal.ZERO : null;
        }

        return currentValue
                .subtract(previousValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousValue, CHANGE_RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Number value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
