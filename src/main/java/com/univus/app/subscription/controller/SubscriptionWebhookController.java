package com.univus.app.subscription.controller;

import com.univus.app.subscription.dto.SubscriptionWebhookRequestDto;
import com.univus.app.subscription.service.SubscriptionBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions/webhooks")
@RequiredArgsConstructor
public class SubscriptionWebhookController {

    private final SubscriptionBillingService subscriptionBillingService;

    @PostMapping("/portone")
    public ResponseEntity<Void> handlePortOneWebhook(
            @RequestBody SubscriptionWebhookRequestDto request
    ) {
        subscriptionBillingService.handleWebhook(request);
        return ResponseEntity.ok().build();
    }
}
