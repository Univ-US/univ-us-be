package com.univus.app.subscription.mapper;

import com.univus.app.subscription.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubscriptionMapper {

    // 구독 플랜 목록을 조회합니다.
    // DELETED_AT이 없는 플랜만 노출합니다.
    List<SubscriptionPlanResponseDto> findActivePlans();

    // 사용자가 선택한 플랜이 실제로 존재하는지 조회합니다.
    // 결제 금액은 프론트 요청값이 아니라 DB의 플랜 가격을 신뢰해야 합니다.
    SubscriptionPlanResponseDto findActivePlanById(@Param("planId") Long planId);

    // 같은 학교명이 이미 구독 진행 중이거나 구독 중인지 확인합니다.
    // 학교를 새로 insert하기 전에 호출해야 중복 학교 구독을 막을 수 있습니다.
    int countPendingOrActiveSubscriptionByUnivName(@Param("univName") String univName);

    // 구독 신청 과정에서 학교를 먼저 생성합니다.
    // insert 후 selectKey로 dto.univId가 채워집니다.
    int insertUniversity(SubscriptionUniversityDto university);

    // 결제 전 PENDING 상태의 구독을 생성합니다.
    // 실제 결제 성공 전이므로 아직 ACTIVE가 아닙니다.
    int insertSubscription(SubscriptionInsertDto subscription);

    // 결제 요청 전에 READY 상태의 결제 이력을 생성합니다.
    // PortOne에 넘길 merchantUid를 먼저 저장해 결제 검증 시 대조합니다.
    int insertPaymentHistory(SubscriptionPaymentHistoryInsertDto paymentHistory);

    // 이미 활성 또는 결제 대기 중인 구독이 있는지 확인합니다.
    // 같은 계정으로 중복 구독 신청을 막기 위해 사용합니다.
    int countPendingOrActiveSubscriptionByMemberId(@Param("memberId") Long memberId);

    // merchantUid로 결제 검증 대상 데이터를 조회합니다.
    // 결제 이력, 구독, 금액, 상태를 한 번에 가져와서 검증 로직에서 사용합니다.
    SubscriptionPaymentVerifyTargetDto findPaymentVerifyTargetByMerchantUid(
            @Param("merchantUid") String merchantUid
    );

    // 결제 검증 성공 시 결제 이력을 PAID 상태로 변경합니다.
    // PortOne 결제 ID와 결제 완료 시각도 함께 저장합니다.
    int markPaymentHistoryPaid(
            @Param("historyId") Long historyId,
            @Param("portonePaymentId") String portonePaymentId
    );

    // 결제 검증 실패 시 결제 이력을 FAILED 상태로 변경합니다.
    // 실패 사유를 failReason에 저장합니다.
    int markPaymentHistoryFailed(
            @Param("historyId") Long historyId,
            @Param("failReason") String failReason
    );

    // 결제 검증 성공 시 구독을 ACTIVE 상태로 변경합니다.
    // 다음 결제일은 플랜 주기에 따라 서비스에서 계산해서 넘깁니다.
    int activateSubscription(
            @Param("subscriptionId") Long subscriptionId,
            @Param("nextBillingAt") java.time.LocalDateTime nextBillingAt
    );

    // 구독 결제 성공 후 결제한 회원을 해당 학교 관리자(ADM)로 변경합니다.
    // MEMBER.UNIV_ID에는 새로 생성된 학교 ID를 연결하고,
    // MEMBER.ROLE은 관리자 역할인 ADM으로 변경합니다.
    int updateMemberAsUniversityAdmin(
            @Param("memberId") Long memberId,
            @Param("univId") Long univId
    );
}