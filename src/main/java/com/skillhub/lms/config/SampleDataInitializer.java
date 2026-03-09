package com.skillhub.lms.config;

import com.skillhub.lms.entity.Course;
import com.skillhub.lms.entity.Section;
import com.skillhub.lms.entity.User;
import com.skillhub.lms.entity.Video;
import com.skillhub.lms.repository.CourseRepository;
import com.skillhub.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Inserts sample courses when the database has no courses (e.g. first run).
 * Skipped when profile "seed" is active (YouTube seed is used instead).
 */
@Component
@Profile("!seed")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SampleDataInitializer implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        long count = courseRepository.count();
        if (count >= 6) {
            return;
        }
        log.info("Courses in DB: {}. Ensuring at least 6 sample courses...", count);
        User instructor = userRepository.findByEmail("instructor@skillhub.com")
                .orElseGet(() -> {
                    User u = User.builder()
                            .name("SkillHub Instructor")
                            .email("instructor@skillhub.com")
                            .username("instructor@skillhub.com")
                            .phone("")
                            .passwordHash(passwordEncoder.encode("changeme"))
                            .role(User.Role.INSTRUCTOR)
                            .build();
                    return userRepository.save(u);
                });

        // Java course (add only if missing by slug)
        if (courseRepository.findBySlug("introduction-to-java-programming").isEmpty()) {
        String javaVid1 = "grEKMHGYyns";
        String javaVid2 = "r59xYe3Vyks"; // Java tutorial Part 1
        String javaVid3 = "l1sIDd1F2xE"; // Java tutorial Part 2
        Course javaCourse = Course.builder()
                .title("Introduction to Java Programming")
                .slug("introduction-to-java-programming")
                .description("Learn the basics of Java programming. This sample course includes a few lessons to get you started.")
                .thumbnailUrl("https://img.youtube.com/vi/" + javaVid1 + "/mqdefault.jpg")
                .category("Programming")
                .whatYouLearn("Java basics\nVariables and data types\nControl flow\nObject-oriented concepts")
                .instructor(instructor)
                .price(BigDecimal.ZERO)
                .isPublished(true)
                .build();
        javaCourse = courseRepository.save(javaCourse);
        Section javaSection = Section.builder()
                .course(javaCourse)
                .title("Getting Started")
                .sortOrder(0)
                .build();
        javaCourse.getSections().add(javaSection);
        addVideo(javaSection, 0, "Welcome to Java", "https://www.youtube.com/watch?v=" + javaVid1, 600);
        addVideo(javaSection, 1, "Setting up your environment", "https://www.youtube.com/watch?v=" + javaVid2, 900);
        addVideo(javaSection, 2, "Your first program", "https://www.youtube.com/watch?v=" + javaVid3, 720);
        courseRepository.save(javaCourse);
        }

        if (courseRepository.findBySlug("python-for-beginners").isEmpty()) {
        Course pythonCourse = Course.builder()
                .title("Python for Beginners")
                .slug("python-for-beginners")
                .description("A gentle introduction to Python programming. Perfect for first-time programmers.")
                .thumbnailUrl("https://img.youtube.com/vi/_uQrJ0TkZlc/mqdefault.jpg")
                .category("Programming")
                .whatYouLearn("Python syntax\nVariables and types\nFunctions\nBasic data structures")
                .instructor(instructor)
                .price(BigDecimal.ZERO)
                .isPublished(true)
                .build();
        pythonCourse = courseRepository.save(pythonCourse);
        Section pythonSection = Section.builder()
                .course(pythonCourse)
                .title("Python Basics")
                .sortOrder(0)
                .build();
        pythonCourse.getSections().add(pythonSection);
        addVideo(pythonSection, 0, "Why Python?", "https://www.youtube.com/watch?v=_uQrJ0TkZlc", 2100);
        addVideo(pythonSection, 1, "Variables and types", "https://www.youtube.com/watch?v=khKv-8q7YmY", 1200);
        addVideo(pythonSection, 2, "Functions", "https://www.youtube.com/watch?v=9Os0o3wzS_I", 1080);
        courseRepository.save(pythonCourse);
        }

        if (courseRepository.findBySlug("html-css-javascript").isEmpty()) {
        // HTML + CSS + JS
        String htmlVid = "mU6anWqZJcc";
        Course htmlCourse = Course.builder()
                .title("HTML, CSS & JavaScript")
                .slug("html-css-javascript")
                .description("Learn web development fundamentals: HTML, CSS, and JavaScript.")
                .thumbnailUrl("https://img.youtube.com/vi/" + htmlVid + "/mqdefault.jpg")
                .category("Web Development")
                .whatYouLearn("HTML structure\nCSS styling\nJavaScript basics")
                .instructor(instructor)
                .price(BigDecimal.ZERO)
                .isPublished(true)
                .build();
        htmlCourse = courseRepository.save(htmlCourse);
        Section htmlSection = Section.builder().course(htmlCourse).title("Basics").sortOrder(0).build();
        htmlCourse.getSections().add(htmlSection);
        addVideo(htmlSection, 0, "HTML Crash Course", "https://www.youtube.com/watch?v=" + htmlVid, 600);
        addVideo(htmlSection, 1, "CSS Basics", "https://www.youtube.com/watch?v=1Rs2ND1ryYc", 720);
        addVideo(htmlSection, 2, "JavaScript Intro", "https://www.youtube.com/watch?v=W6NZfCO5SIk", 900);
        courseRepository.save(htmlCourse);
        }

        if (courseRepository.findBySlug("computer-graphics").isEmpty()) {
        // Computer Graphics (use embeddable video IDs to avoid "Video unavailable")
        String cgVid1 = "Tn6-PIqc4UM";
        String cgVid2 = "RGOj5yH7evk";
        Course cgCourse = Course.builder()
                .title("Computer Graphics")
                .slug("computer-graphics")
                .description("Introduction to computer graphics and visualization.")
                .thumbnailUrl("https://img.youtube.com/vi/" + cgVid1 + "/mqdefault.jpg")
                .category("Design")
                .whatYouLearn("Graphics fundamentals\nRendering\n2D/3D concepts")
                .instructor(instructor)
                .price(BigDecimal.ZERO)
                .isPublished(true)
                .build();
        cgCourse = courseRepository.save(cgCourse);
        Section cgSection = Section.builder().course(cgCourse).title("Introduction").sortOrder(0).build();
        cgCourse.getSections().add(cgSection);
        addVideo(cgSection, 0, "Introduction to Computer Graphics", "https://www.youtube.com/watch?v=" + cgVid1, 600);
        addVideo(cgSection, 1, "Graphics Pipeline", "https://www.youtube.com/watch?v=" + cgVid2, 540);
        courseRepository.save(cgCourse);
        }

        if (courseRepository.findBySlug("dbms-sql").isEmpty()) {
        // DBMS / SQL
        String sqlVid = "HXV3zeQKqGY";
        Course dbCourse = Course.builder()
                .title("DBMS & SQL")
                .slug("dbms-sql")
                .description("Database management systems and SQL for beginners.")
                .thumbnailUrl("https://img.youtube.com/vi/" + sqlVid + "/mqdefault.jpg")
                .category("Database")
                .whatYouLearn("Relational databases\nSQL queries\nNormalization")
                .instructor(instructor)
                .price(BigDecimal.ZERO)
                .isPublished(true)
                .build();
        dbCourse = courseRepository.save(dbCourse);
        Section dbSection = Section.builder().course(dbCourse).title("SQL Basics").sortOrder(0).build();
        dbCourse.getSections().add(dbSection);
        addVideo(dbSection, 0, "SQL Tutorial", "https://www.youtube.com/watch?v=" + sqlVid, 2700);
        addVideo(dbSection, 1, "SELECT and WHERE", "https://www.youtube.com/watch?v=2-1XQHAgDsM", 900);
        courseRepository.save(dbCourse);
        }

        if (courseRepository.findBySlug("devops-bootcamp").isEmpty()) {
        // DevOps Bootcamp (separate course; playlist PL9gnSGHSqcnoqBXdMwUTRod4Gi3eac2Ak has 7 videos)
        String devOpsVid = "lJBgSwgDTT0";
        Course devOpsCourse = Course.builder()
                .title("DevOps Bootcamp")
                .slug("devops-bootcamp")
                .description("DevOps practices: CI/CD, containers, and modern deployment.")
                .thumbnailUrl("https://img.youtube.com/vi/" + devOpsVid + "/mqdefault.jpg")
                .category("Development")
                .whatYouLearn("CI/CD\nContainers\nDeployment pipelines")
                .instructor(instructor)
                .price(BigDecimal.ZERO)
                .isPublished(true)
                .build();
        devOpsCourse = courseRepository.save(devOpsCourse);
        Section devOpsSection = Section.builder().course(devOpsCourse).title("Videos").sortOrder(0).build();
        devOpsCourse.getSections().add(devOpsSection);
        addVideo(devOpsSection, 0, "DevOps in 10 Minutes", "https://www.youtube.com/watch?v=" + devOpsVid, 600);
        addVideo(devOpsSection, 1, "Docker Basics", "https://www.youtube.com/watch?v=Gjnup-PuquQ", 720);
        courseRepository.save(devOpsCourse);
        }

        if (courseRepository.findBySlug("java-dsa-interview-preparation").isEmpty()) {
        // Java + DSA + Interview Preparation (separate course; playlist PL9gnSGHSqcnr_DxHsP7AW9ftq0AtAyYqJ has 69 videos)
        String javaDsaVid = "grEKMHGYyns";
        Course javaDsaCourse = Course.builder()
                .title("Java + DSA + Interview Preparation")
                .slug("java-dsa-interview-preparation")
                .description("Data structures, algorithms, and interview preparation in Java.")
                .thumbnailUrl("https://img.youtube.com/vi/" + javaDsaVid + "/mqdefault.jpg")
                .category("Programming")
                .whatYouLearn("Data structures\nAlgorithms\nInterview preparation")
                .instructor(instructor)
                .price(BigDecimal.ZERO)
                .isPublished(true)
                .build();
        javaDsaCourse = courseRepository.save(javaDsaCourse);
        Section javaDsaSection = Section.builder().course(javaDsaCourse).title("Videos").sortOrder(0).build();
        javaDsaCourse.getSections().add(javaDsaSection);
        addVideo(javaDsaSection, 0, "Introduction to Java & DSA", "https://www.youtube.com/watch?v=" + javaDsaVid, 600);
        addVideo(javaDsaSection, 1, "Arrays and Strings", "https://www.youtube.com/watch?v=r59xYe3Vyks", 900);
        courseRepository.save(javaDsaCourse);
        }

        log.info("Sample data: ensured at least 6 courses with videos (total now: {}).", courseRepository.count());
    }

    private void addVideo(Section section, int orderIndex, String title, String youtubeUrl, int durationSeconds) {
        Video video = Video.builder()
                .section(section)
                .title(title)
                .youtubeUrl(youtubeUrl)
                .orderIndex(orderIndex)
                .durationSeconds(durationSeconds)
                .build();
        section.getVideos().add(video);
    }
}
