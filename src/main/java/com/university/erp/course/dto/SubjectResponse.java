package com.university.erp.course.dto;

public record SubjectResponse(Long id, String subjectCode, String subjectName, Long courseId, Short inSemester,
        Short credits, String subjectType, Integer subjectHours) {
}
