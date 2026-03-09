package com.skillhub.lms.controller;

import com.skillhub.lms.service.EnrollmentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final EnrollmentService enrollmentService;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Stripe webhook secret not configured");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
            if (stripeObject instanceof com.stripe.model.checkout.Session session) {
                String userIdStr = session.getMetadata().get("userId");
                String courseIdStr = session.getMetadata().get("courseId");
                if (userIdStr != null && courseIdStr != null) {
                    Long userId = Long.parseLong(userIdStr);
                    Long courseId = Long.parseLong(courseIdStr);
                    enrollmentService.createEnrollmentAfterPayment(userId, courseId, session.getPaymentStatus());
                }
            }
        }
        return ResponseEntity.ok().build();
    }
}
