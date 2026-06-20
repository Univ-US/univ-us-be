package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminBulkSignupDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceAdminBulkSignupMapper {

    ServiceAdminBulkSignupDto.Capacity selectActiveCapacity(
            @Param("univId") Long univId
    );

    List<Long> selectDepartmentIdsByUniversity(
            @Param("univId") Long univId
    );

    List<String> selectExistingLoginIds(
            @Param("loginIds") List<String> loginIds
    );
}
