package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminDashboardDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ServiceAdminDashboardMapper {

    ServiceAdminDashboardDto.Summary selectDashboardSummary();

    List<ServiceAdminDashboardDto.SchoolOverview> selectSchoolOverview();

    List<ServiceAdminDashboardDto.RecentMember> selectRecentMembers();
}
