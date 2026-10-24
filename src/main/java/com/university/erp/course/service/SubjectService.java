package com.university.erp.course.service;

import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectRequest;
import com.university.erp.course.dto.SubjectResponse;
import java.util.List;

public interface SubjectService {
    SubjectResponse create(SubjectRequest request);

    SubjectResponse get(Long id);

    SubjectResponse getBySubjectCode(String subjectCode);

    List<SubjectResponse> list(Long courseId, Short inSemester, String subjectType);

    CourseResponse getCourseBySubjectId(Long id);

    SubjectResponse update(Long id, SubjectRequest request);

    void delete(Long id);
}
