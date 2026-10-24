package com.university.erp.course.mapper;

import com.university.erp.course.dto.CourseRequest;
import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.entity.Course;

public final class CourseMapper {
    private CourseMapper() {
    }

    public static Course toEntity(CourseRequest request) {
        if (request == null) {
            return null;
        }
        Course course = new Course();
        apply(request, course);
        return course;
    }

    public static CourseResponse toResponse(Course course) {
        if (course == null) {
            return null;
        }
        return new CourseResponse(course.getId(), course.getCourseCode(), course.getCourseName(),
                course.getCourseType(), course.getCourseDuration(), course.getDepartmentId());
    }

    public static void apply(CourseRequest request, Course course) {
        if (request == null || course == null) {
            return;
        }
        course.setCourseCode(request.courseCode());
        course.setCourseName(request.courseName());
        course.setCourseType(request.courseType());
        course.setCourseDuration(request.courseDuration());
        course.setDepartmentId(request.departmentId());
    }
}
