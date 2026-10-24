package com.university.erp.course.repository;

import com.university.erp.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);

    boolean existsByCourseCodeAndIdNot(String courseCode, Long id);

    List<Course> findByDepartmentId(Integer departmentId);

    List<Course> findByCourseType(String courseType);

    List<Course> findByDepartmentIdAndCourseType(Integer departmentId, String courseType);
}
