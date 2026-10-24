package com.university.erp.course.dto;

public record CourseRequest(String courseCode, String courseName, String courseType, Integer courseDuration,
        Integer departmentId) {
}
