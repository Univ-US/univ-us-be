package com.univus.app.subscription.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubscriptionMemberLimitMapper {

    Long selectMaxMemberCount(@Param("univId") Long univId);

    long countBillableMembers(@Param("univId") Long univId);
}
