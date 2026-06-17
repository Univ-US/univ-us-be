package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminOperationLogDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ServiceAdminOperationLogMapper {

    List<ServiceAdminOperationLogDto.Log> selectOperationLogs(
            ServiceAdminOperationLogDto.Search search
    );

    long countOperationLogs(ServiceAdminOperationLogDto.Search search);

    ServiceAdminOperationLogDto.Summary selectOperationLogSummary(
            ServiceAdminOperationLogDto.Search search
    );
}
