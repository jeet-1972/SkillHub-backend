package com.skillhub.lms.service;

import com.skillhub.lms.entity.Course;
import com.skillhub.lms.repository.CourseRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CourseRepository courseRepository;

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.stripe.success-url:http://localhost:5173/dashboard}")
    private String successUrl;

    @Value("${app.stripe.cancel-url:http://localhost:5173/courses}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    public String createCheckoutSession(Long userId, Long courseId, String baseUrl) throws StripeException {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe is not configured");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (course.getPrice() == null || course.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Course is free");
        }
        long amountCents = course.getPrice().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl((baseUrl != null ? baseUrl : successUrl) + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(course.getTitle())
                                        .build())
                                .build())
                        .build())
                .putMetadata("userId", String.valueOf(userId))
                .putMetadata("courseId", String.valueOf(courseId))
                .build();
        Session session = Session.create(params);
        return session.getUrl();
    }
}
