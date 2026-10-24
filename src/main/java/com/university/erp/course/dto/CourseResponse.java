package com.university.erp.course.dto;

public record CourseResponse(Long id, String courseCode, String courseName, String courseType, Integer courseDuration,
        Integer departmentId) {
}
