package com.skillhub.lms.controller;

import com.skillhub.lms.dto.EnrollmentDto;
import com.skillhub.lms.dto.EnrollmentResponseDto;
import com.skillhub.lms.security.UserPrincipal;
import com.skillhub.lms.service.EnrollmentService;
import com.skillhub.lms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponseDto> enroll(@RequestBody Map<String, Long> body, @AuthenticationPrincipal UserPrincipal principal,
                                       HttpServletRequest request) {
        Long courseId = body.get("courseId");
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        EnrollmentService.EnrollmentResult result = enrollmentService.enroll(principal.getUserId(), courseId);
        if (result.enrolled) {
            return ResponseEntity.status(HttpStatus.CREATED).body(EnrollmentResponseDto.builder().enrolled(true).build());
        }
        if (result.requiresPayment) {
            try {
                String origin = request.getHeader("Origin");
                String baseUrl = origin != null ? origin : "http://localhost:5173";
                String checkoutUrl = paymentService.createCheckoutSession(principal.getUserId(), courseId, baseUrl + "/dashboard");
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(EnrollmentResponseDto.builder().enrolled(false).checkoutUrl(checkoutUrl).build());
            } catch (Exception e) {
                throw new IllegalStateException("Payment not configured or failed", e);
            }
        }
        throw new IllegalStateException("Enrollment failed");
    }

    @GetMapping
    public List<EnrollmentDto> myEnrollments(@AuthenticationPrincipal UserPrincipal principal) {
        return enrollmentService.getMyEnrollments(principal.getUserId());
    }
}
