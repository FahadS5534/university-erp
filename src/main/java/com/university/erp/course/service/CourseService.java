package com.university.erp.course.service;

import com.university.erp.course.dto.CourseRequest;
import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectResponse;
import java.util.List;

public interface CourseService {
    CourseResponse create(CourseRequest request);

    CourseResponse get(Long id);

    CourseResponse getByCourseCode(String courseCode);

    List<CourseResponse> list(Integer departmentId, String courseType);

    CourseResponse update(Long id, CourseRequest request);

    void delete(Long id);

    List<SubjectResponse> getSubjectsByCourseId(Long courseId);
}
