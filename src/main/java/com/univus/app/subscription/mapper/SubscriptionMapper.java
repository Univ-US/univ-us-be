package com.univus.app.subscription.mapper;

import com.univus.app.subscription.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SubscriptionMapper {

    // 구독 플랜 목록을 조회합니다.
    // DELETED_AT이 없는 플랜만 노출합니다.
    List<SubscriptionPlanResponseDto> findActivePlans();

    // 사용자가 선택한 플랜이 실제로 존재하는지 조회합니다.
    // 결제 금액은 프론트 요청값이 아니라 DB의 플랜 가격을 신뢰해야 합니다.
    SubscriptionPlanResponseDto findActivePlanById(@Param("planId") Long planId);

    List<SubscriptionUniversityOptionDto> searchUniversities(@Param("keyword") String keyword);

    SubscriptionUniversityOptionDto findUniversityOptionById(@Param("univId") Long univId);

    SubscriptionAccessStatusDto findSubscriptionAccessStatus(
            @Param("memberId") Long memberId
    );

    // 같은 학교명이 이미 구독 진행 중이거나 구독 중인지 확인합니다.
    // 기존 방식으로 생성된 PENDING 학교와 현재 ACTIVE 학교의 중복을 막습니다.
    int countPendingOrActiveSubscriptionByUnivName(@Param("univName") String univName);

    // 같은 학교명으로 진행 중인 신청이 있는지 확인합니다.
    int countPendingSubscriptionApplicationByUnivName(@Param("univName") String univName);

    // 결제 검증 성공 후 학교를 생성합니다.
    // insert 후 selectKey로 dto.univId가 채워집니다.
    int insertUniversity(SubscriptionUniversityDto university);

    // 결제 전 PENDING 상태의 구독을 생성합니다.
    // 실제 결제 성공 전이므로 아직 ACTIVE가 아닙니다.
    int insertSubscription(SubscriptionInsertDto subscription);

    // 결제 전 학교 신청 정보를 PENDING 상태로 저장합니다.
    int insertSubscriptionApplication(SubscriptionApplicationDto application);

    // 결제 요청 전에 READY 상태의 결제 이력을 생성합니다.
    // PortOne에 넘길 merchantUid를 먼저 저장해 결제 검증 시 대조합니다.
    int insertPaymentHistory(SubscriptionPaymentHistoryInsertDto paymentHistory);

    int insertBillingKey(SubscriptionBillingKeyInsertDto billingKey);

    Long findBillingKeyIdByMemberId(@Param("memberId") Long memberId);

    int updateBillingKeyForResubscription(SubscriptionBillingKeyInsertDto billingKey);

    int attachSubscriptionBillingKey(
            @Param("subscriptionId") Long subscriptionId,
            @Param("billingKeyId") Long billingKeyId
    );

    int attachPaymentHistoryBillingKey(
            @Param("historyId") Long historyId,
            @Param("billingKeyId") Long billingKeyId
    );

    // 이미 활성 또는 결제 대기 중인 구독이 있는지 확인합니다.
    // 같은 계정으로 중복 구독 신청을 막기 위해 사용합니다.
    int countPendingOrActiveSubscriptionByMemberId(@Param("memberId") Long memberId);

    int countPendingOrActiveSubscriptionByUnivId(@Param("univId") Long univId);

    // merchantUid로 결제 검증 대상 데이터를 조회합니다.
    // 결제 이력, 구독, 금액, 상태를 한 번에 가져와서 검증 로직에서 사용합니다.
    SubscriptionPaymentVerifyTargetDto findPaymentVerifyTargetByMerchantUid(
            @Param("merchantUid") String merchantUid
    );

    SubscriptionRenewalTargetDto findRenewalTargetByMerchantUid(
            @Param("merchantUid") String merchantUid
    );

    // 결제 검증 성공 시 결제 이력을 PAID 상태로 변경합니다.
    // PortOne 결제 ID와 결제 완료 시각도 함께 저장합니다.
    int markPaymentHistoryPaid(
            @Param("historyId") Long historyId,
            @Param("portonePaymentId") String portonePaymentId,
            @Param("paidAt") LocalDateTime paidAt,
            @Param("univId") Long univId
    );

    // 결제 검증 실패 시 결제 이력을 FAILED 상태로 변경합니다.
    // 실패 사유를 failReason에 저장합니다.
    int markPaymentHistoryFailed(
            @Param("historyId") Long historyId,
            @Param("failReason") String failReason
    );

    int markRenewalPaymentPaid(
            @Param("historyId") Long historyId,
            @Param("portonePaymentId") String portonePaymentId,
            @Param("paidAt") LocalDateTime paidAt
    );

    int updateNextBillingAt(
            @Param("subscriptionId") Long subscriptionId,
            @Param("nextBillingAt") LocalDateTime nextBillingAt
    );

    int updatePaymentScheduleId(
            @Param("historyId") Long historyId,
            @Param("portoneScheduleId") String portoneScheduleId
    );

    // 결제 검증 성공 시 구독을 ACTIVE 상태로 변경합니다.
    // 다음 결제일은 플랜 주기에 따라 서비스에서 계산해서 넘깁니다.
    int activateSubscription(
            @Param("subscriptionId") Long subscriptionId,
            @Param("univId") Long univId,
            @Param("nextBillingAt") java.time.LocalDateTime nextBillingAt
    );

    // 결제 검증 성공 후 신청을 COMPLETED 상태로 변경합니다.
    int completeSubscriptionApplication(@Param("applicationId") Long applicationId);

    // 결제창 취소 시 신청을 CANCELED 상태로 변경합니다.
    int cancelSubscriptionApplication(
            @Param("applicationId") Long applicationId,
            @Param("failReason") String failReason
    );

    // 결제 검증 실패 시 신청을 FAILED 상태로 변경합니다.
    int failSubscriptionApplication(
            @Param("applicationId") Long applicationId,
            @Param("failReason") String failReason
    );

    // 결제창 취소 또는 결제 진행 실패 시 결제 이력을 CANCELED 상태로 변경합니다.
    // prepare 단계에서 READY로 생성된 결제 이력을 닫아 재시도 가능 상태로 만들기 위해 사용합니다.
    int markPaymentHistoryCanceled(
            @Param("historyId") Long historyId,
            @Param("failReason") String failReason
    );

    // 결제창 취소 또는 결제 진행 실패 시 PENDING 상태의 구독을 CANCELED 상태로 변경합니다.
    // 결제되지 않은 구독 신청 데이터를 닫아 같은 사용자가 다시 구독 신청을 할 수 있게 합니다.
    int cancelSubscription(
            @Param("subscriptionId") Long subscriptionId
    );

    // 구독 결제 성공 후 결제한 회원을 해당 학교 관리자(ADM)로 변경합니다.
    // MEMBER.UNIV_ID에는 새로 생성된 학교 ID를 연결하고,
    // MEMBER.ROLE은 관리자 역할인 ADM으로 변경합니다.
    int updateMemberAsUniversityAdmin(
            @Param("memberId") Long memberId,
            @Param("univId") Long univId
    );
}
