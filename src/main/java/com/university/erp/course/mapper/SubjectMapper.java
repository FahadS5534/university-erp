package com.university.erp.course.mapper;

import com.university.erp.course.dto.SubjectRequest;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.entity.Subject;

public final class SubjectMapper {
    private SubjectMapper() {
    }

    public static Subject toEntity(SubjectRequest request) {
        if (request == null) {
            return null;
        }
        Subject subject = new Subject();
        apply(request, subject);
        return subject;
    }

    public static SubjectResponse toResponse(Subject subject) {
        if (subject == null) {
            return null;
        }
        return new SubjectResponse(subject.getId(), subject.getSubjectCode(), subject.getSubjectName(),
                subject.getCourseId(), subject.getInSemester(), subject.getCredits(), subject.getSubjectType(),
                subject.getSubjectHours());
    }

    public static void apply(SubjectRequest request, Subject subject) {
        if (request == null || subject == null) {
            return;
        }
        subject.setSubjectCode(request.subjectCode());
        subject.setSubjectName(request.subjectName());
        subject.setCourseId(request.courseId());
        subject.setInSemester(request.inSemester());
        subject.setCredits(request.credits());
        subject.setSubjectType(request.subjectType());
        subject.setSubjectHours(request.subjectHours());
    }
}
