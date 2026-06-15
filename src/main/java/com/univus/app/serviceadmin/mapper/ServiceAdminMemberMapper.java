package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceAdminMemberMapper {

    List<ServiceAdminMemberDto.Member> selectMembers(
            ServiceAdminMemberDto.Search search
    );

    long countMembers(ServiceAdminMemberDto.Search search);

    ServiceAdminMemberDto.Stats selectMemberStats();

    ServiceAdminMemberDto.Member selectMemberById(
            @Param("memberId") Long memberId
    );

    int updateMemberStatus(
            @Param("memberId") Long memberId,
            @Param("status") String status
    );
}
