package com.skillhub.lms.controller;

import com.skillhub.lms.security.UserPrincipal;
import com.skillhub.lms.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public Map<String, String> checkout(@RequestBody Map<String, Long> body, @AuthenticationPrincipal UserPrincipal principal,
                                        @RequestHeader(value = "Origin", required = false) String origin) throws StripeException {
        Long courseId = body.get("courseId");
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        String successUrl = origin != null ? origin + "/dashboard" : "http://localhost:5173/dashboard";
        String url = paymentService.createCheckoutSession(principal.getUserId(), courseId, successUrl);
        return Map.of("checkoutUrl", url);
    }
}
