package com.univus.app.admin.mapper;

import com.univus.app.admin.dto.SchoolAdminReservationPenaltyDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SchoolAdminReservationPenaltyMapper {

    List<SchoolAdminReservationPenaltyDto.Penalty> selectPenalties(
            SchoolAdminReservationPenaltyDto.PenaltySearch search);

    long countPenalties(SchoolAdminReservationPenaltyDto.PenaltySearch search);

    SchoolAdminReservationPenaltyDto.Penalty selectPenaltyById(@Param("penaltyId") Long penaltyId);

    SchoolAdminReservationPenaltyDto.Penalty selectLatestPenaltyForMember(@Param("memberId") Long memberId);

    int releasePenalty(@Param("penaltyId") Long penaltyId);
}
