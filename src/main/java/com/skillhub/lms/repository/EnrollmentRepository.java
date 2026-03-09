package com.skillhub.lms.repository;

import com.skillhub.lms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    void deleteByCourse_Id(Long courseId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c LEFT JOIN FETCH c.instructor WHERE e.user.id = :userId")
    List<Enrollment> findByUserIdWithCourseAndInstructor(Long userId);
}
