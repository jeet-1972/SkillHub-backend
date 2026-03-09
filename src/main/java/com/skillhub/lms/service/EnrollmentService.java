package com.skillhub.lms.service;

import com.skillhub.lms.dto.EnrollmentDto;
import com.skillhub.lms.entity.Course;
import com.skillhub.lms.entity.Enrollment;
import com.skillhub.lms.entity.User;
import com.skillhub.lms.repository.CourseRepository;
import com.skillhub.lms.repository.EnrollmentRepository;
import com.skillhub.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public EnrollmentResult enroll(Long userId, Long courseId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalArgumentException("Already enrolled");
        }
        if (course.getPrice() == null || course.getPrice().signum() == 0) {
            Enrollment enrollment = Enrollment.builder()
                    .user(user)
                    .course(course)
                    .build();
            enrollmentRepository.save(enrollment);
            return EnrollmentResult.enrolled();
        }
        return EnrollmentResult.requiresPayment();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto> getMyEnrollments(Long userId) {
        return enrollmentRepository.findByUserIdWithCourseAndInstructor(userId).stream()
                .map(e -> EnrollmentDto.builder()
                        .id(e.getId())
                        .courseId(e.getCourse().getId())
                        .courseTitle(e.getCourse().getTitle())
                        .thumbnailUrl(e.getCourse().getThumbnailUrl())
                        .instructorName(e.getCourse().getInstructor().getName())
                        .enrolledAt(e.getEnrolledAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Transactional
    public void createEnrollmentAfterPayment(Long userId, Long courseId, String paymentId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return;
        }
        enrollmentRepository.save(Enrollment.builder()
                .user(user)
                .course(course)
                .paymentId(paymentId)
                .build());
    }

    public static class EnrollmentResult {
        public final boolean enrolled;
        public final boolean requiresPayment;

        public static EnrollmentResult enrolled() {
            EnrollmentResult r = new EnrollmentResult(true, false);
            return r;
        }

        public static EnrollmentResult requiresPayment() {
            return new EnrollmentResult(false, true);
        }

        private EnrollmentResult(boolean enrolled, boolean requiresPayment) {
            this.enrolled = enrolled;
            this.requiresPayment = requiresPayment;
        }
    }
}
