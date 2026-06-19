package com.univus.app.serviceadmin.mapper;

import com.univus.app.serviceadmin.dto.ServiceAdminReservationPenaltyDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceAdminReservationPenaltyMapper {

    List<ServiceAdminReservationPenaltyDto.Penalty> selectPenalties(
            ServiceAdminReservationPenaltyDto.PenaltySearch search);

    long countPenalties(ServiceAdminReservationPenaltyDto.PenaltySearch search);

    ServiceAdminReservationPenaltyDto.Penalty selectPenaltyById(@Param("penaltyId") Long penaltyId);

    ServiceAdminReservationPenaltyDto.Penalty selectLatestPenaltyForMember(@Param("memberId") Long memberId);

    int releasePenalty(@Param("penaltyId") Long penaltyId);
}
