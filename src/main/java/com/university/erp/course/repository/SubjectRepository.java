package com.university.erp.course.repository;

import com.university.erp.course.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findBySubjectCode(String subjectCode);

    boolean existsBySubjectCode(String subjectCode);

    boolean existsBySubjectCodeAndIdNot(String subjectCode, Long id);

    List<Subject> findByCourseId(Long courseId);

    List<Subject> findByCourseIdAndInSemester(Long courseId, Short inSemester);

    List<Subject> findBySubjectType(String subjectType);

    boolean existsByCourseId(Long courseId);
}
