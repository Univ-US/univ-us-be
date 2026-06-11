package com.univus.app.subscription.service;

import com.univus.app.security.JwtTokenProvider;
import com.univus.app.subscription.dto.SubscriptionApplicationDto;
import com.univus.app.subscription.dto.SubscriptionInsertDto;
import com.univus.app.subscription.dto.SubscriptionPaymentCancelRequestDto;
import com.univus.app.subscription.dto.SubscriptionPaymentHistoryInsertDto;
import com.univus.app.subscription.dto.SubscriptionPaymentVerifyRequestDto;
import com.univus.app.subscription.dto.SubscriptionPaymentVerifyResponseDto;
import com.univus.app.subscription.dto.SubscriptionPaymentVerifyTargetDto;
import com.univus.app.subscription.dto.SubscriptionPlanResponseDto;
import com.univus.app.subscription.dto.SubscriptionPrepareRequestDto;
import com.univus.app.subscription.dto.SubscriptionPrepareResponseDto;
import com.univus.app.subscription.dto.SubscriptionUniversityDto;
import com.univus.app.subscription.mapper.SubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    @Mock
    private SubscriptionPaymentFailureRecorder failureRecorder;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void prepareStoresApplicationWithoutCreatingUniversity() {
        SubscriptionPrepareRequestDto request = prepareRequest();
        SubscriptionPlanResponseDto plan = SubscriptionPlanResponseDto.builder()
                .planId(1L)
                .planName("BASIC")
                .price(10000L)
                .billingCycle("MONTHLY")
                .build();

        when(subscriptionMapper.findActivePlanById(1L)).thenReturn(plan);
        when(subscriptionMapper.countPendingOrActiveSubscriptionByMemberId(10L)).thenReturn(0);
        when(subscriptionMapper.countPendingOrActiveSubscriptionByUnivName("UnivUs University"))
                .thenReturn(0);
        when(subscriptionMapper.countPendingSubscriptionApplicationByUnivName("UnivUs University"))
                .thenReturn(0);
        when(subscriptionMapper.insertSubscription(any(SubscriptionInsertDto.class)))
                .thenAnswer(invocation -> {
                    SubscriptionInsertDto dto = invocation.getArgument(0);
                    dto.setSubscriptionId(100L);
                    return 1;
                });
        when(subscriptionMapper.insertSubscriptionApplication(any(SubscriptionApplicationDto.class)))
                .thenAnswer(invocation -> {
                    SubscriptionApplicationDto dto = invocation.getArgument(0);
                    dto.setApplicationId(200L);
                    return 1;
                });
        when(subscriptionMapper.insertPaymentHistory(any(SubscriptionPaymentHistoryInsertDto.class)))
                .thenAnswer(invocation -> {
                    SubscriptionPaymentHistoryInsertDto dto = invocation.getArgument(0);
                    dto.setHistoryId(300L);
                    return 1;
                });

        SubscriptionPrepareResponseDto response =
                subscriptionService.prepareSubscription(10L, request);

        ArgumentCaptor<SubscriptionInsertDto> subscriptionCaptor =
                ArgumentCaptor.forClass(SubscriptionInsertDto.class);
        ArgumentCaptor<SubscriptionApplicationDto> applicationCaptor =
                ArgumentCaptor.forClass(SubscriptionApplicationDto.class);
        ArgumentCaptor<SubscriptionPaymentHistoryInsertDto> paymentCaptor =
                ArgumentCaptor.forClass(SubscriptionPaymentHistoryInsertDto.class);

        verify(subscriptionMapper).insertSubscription(subscriptionCaptor.capture());
        verify(subscriptionMapper).insertSubscriptionApplication(applicationCaptor.capture());
        verify(subscriptionMapper).insertPaymentHistory(paymentCaptor.capture());
        verify(subscriptionMapper, never()).insertUniversity(any(SubscriptionUniversityDto.class));

        assertNull(subscriptionCaptor.getValue().getUnivId());
        assertNull(paymentCaptor.getValue().getUnivId());
        assertEquals(100L, applicationCaptor.getValue().getSubscriptionId());
        assertEquals("PENDING", applicationCaptor.getValue().getStatus());
        assertEquals("UnivUs University", applicationCaptor.getValue().getUnivName());
        assertEquals(100L, response.getSubscriptionId());
        assertEquals(300L, response.getPaymentHistoryId());
    }

    @Test
    void verifyCreatesUniversityAndCompletesApplicationAfterPaymentValidation() {
        SubscriptionPaymentVerifyTargetDto target = pendingTarget();
        SubscriptionPaymentVerifyRequestDto request = new SubscriptionPaymentVerifyRequestDto();
        request.setMerchantUid("sub-payment-1");
        request.setPortonePaymentId("sub-payment-1");

        PortOnePaymentClient.PortOnePayment payment =
                org.mockito.Mockito.mock(PortOnePaymentClient.PortOnePayment.class);
        PortOnePaymentClient.PortOnePayment.Amount amount =
                org.mockito.Mockito.mock(PortOnePaymentClient.PortOnePayment.Amount.class);

        when(subscriptionMapper.findPaymentVerifyTargetByMerchantUid("sub-payment-1"))
                .thenReturn(target);
        when(portOnePaymentClient.getPayment("sub-payment-1")).thenReturn(payment);
        when(payment.getId()).thenReturn("sub-payment-1");
        when(payment.getStatus()).thenReturn("PAID");
        when(payment.getAmount()).thenReturn(amount);
        when(amount.getTotal()).thenReturn(10000L);
        when(payment.getPaidAt()).thenReturn("2026-06-11T01:00:00Z");
        when(subscriptionMapper.insertUniversity(any(SubscriptionUniversityDto.class)))
                .thenAnswer(invocation -> {
                    SubscriptionUniversityDto dto = invocation.getArgument(0);
                    dto.setUnivId(500L);
                    return 1;
                });
        when(subscriptionMapper.markPaymentHistoryPaid(
                anyLong(), anyString(), any(LocalDateTime.class), anyLong()
        )).thenReturn(1);
        when(subscriptionMapper.activateSubscription(
                anyLong(), anyLong(), any(LocalDateTime.class)
        )).thenReturn(1);
        when(subscriptionMapper.updateMemberAsUniversityAdmin(10L, 500L)).thenReturn(1);
        when(subscriptionMapper.completeSubscriptionApplication(200L)).thenReturn(1);
        when(jwtTokenProvider.createAccessToken(10L, "ADM")).thenReturn("new-access-token");
        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");

        SubscriptionPaymentVerifyResponseDto response =
                subscriptionService.verifySubscriptionPayment(10L, request);

        InOrder order = inOrder(subscriptionMapper);
        order.verify(subscriptionMapper).insertUniversity(any(SubscriptionUniversityDto.class));
        order.verify(subscriptionMapper).markPaymentHistoryPaid(
                anyLong(), anyString(), any(LocalDateTime.class), anyLong()
        );
        order.verify(subscriptionMapper).activateSubscription(
                anyLong(), anyLong(), any(LocalDateTime.class)
        );
        order.verify(subscriptionMapper).updateMemberAsUniversityAdmin(10L, 500L);
        order.verify(subscriptionMapper).completeSubscriptionApplication(200L);

        assertEquals(500L, response.getUnivId());
        assertEquals("PAID", response.getPaymentStatus());
        assertEquals("ACTIVE", response.getSubscriptionStatus());
        assertEquals("ADM", response.getRole());
        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    void verifyFailureDelegatesAtomicFailureCleanup() {
        SubscriptionPaymentVerifyTargetDto target = pendingTarget();
        SubscriptionPaymentVerifyRequestDto request = new SubscriptionPaymentVerifyRequestDto();
        request.setMerchantUid("sub-payment-1");
        request.setPortonePaymentId("sub-payment-1");

        when(subscriptionMapper.findPaymentVerifyTargetByMerchantUid("sub-payment-1"))
                .thenReturn(target);
        when(portOnePaymentClient.getPayment("sub-payment-1"))
                .thenThrow(new RestClientException("network error"));

        assertThrows(
                IllegalStateException.class,
                () -> subscriptionService.verifySubscriptionPayment(10L, request)
        );

        verify(failureRecorder).markFailed(
                300L,
                100L,
                200L,
                "PortOne payment lookup failed."
        );
        verify(subscriptionMapper, never()).insertUniversity(any(SubscriptionUniversityDto.class));
    }

    @Test
    void cancelClosesReadyPaymentSubscriptionAndApplication() {
        SubscriptionPaymentVerifyTargetDto target = pendingTarget();
        SubscriptionPaymentCancelRequestDto request = new SubscriptionPaymentCancelRequestDto();
        request.setMerchantUid("sub-payment-1");
        request.setReason("USER_CANCELLED");

        when(subscriptionMapper.findPaymentVerifyTargetByMerchantUid("sub-payment-1"))
                .thenReturn(target);
        when(subscriptionMapper.markPaymentHistoryCanceled(300L, "USER_CANCELLED")).thenReturn(1);
        when(subscriptionMapper.cancelSubscription(100L)).thenReturn(1);
        when(subscriptionMapper.cancelSubscriptionApplication(200L, "USER_CANCELLED")).thenReturn(1);

        subscriptionService.cancelPreparedSubscriptionPayment(10L, request);

        verify(subscriptionMapper).markPaymentHistoryCanceled(300L, "USER_CANCELLED");
        verify(subscriptionMapper).cancelSubscription(100L);
        verify(subscriptionMapper).cancelSubscriptionApplication(200L, "USER_CANCELLED");
    }

    private SubscriptionPrepareRequestDto prepareRequest() {
        SubscriptionPrepareRequestDto request = new SubscriptionPrepareRequestDto();
        request.setPlanId(1L);
        request.setUnivName("UnivUs University");
        request.setSido("Seoul");
        request.setAddress("1 UnivUs-ro");
        request.setSchoolPhone("02-0000-0000");
        request.setHomepage("https://univus.example");
        return request;
    }

    private SubscriptionPaymentVerifyTargetDto pendingTarget() {
        SubscriptionPaymentVerifyTargetDto target = new SubscriptionPaymentVerifyTargetDto();
        target.setPaymentHistoryId(300L);
        target.setSubscriptionId(100L);
        target.setMemberId(10L);
        target.setApplicationId(200L);
        target.setApplicationMemberId(10L);
        target.setApplicationPlanId(1L);
        target.setPlanId(1L);
        target.setMerchantUid("sub-payment-1");
        target.setAmount(10000L);
        target.setPaymentStatus("READY");
        target.setSubscriptionStatus("PENDING");
        target.setApplicationStatus("PENDING");
        target.setUnivName("UnivUs University");
        target.setSido("Seoul");
        target.setAddress("1 UnivUs-ro");
        target.setSchoolPhone("02-0000-0000");
        target.setHomepage("https://univus.example");
        return target;
    }
}
