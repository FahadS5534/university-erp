package com.university.erp.course.service;

import com.university.erp.course.dto.CourseRequest;
import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.entity.Course;
import com.university.erp.course.mapper.CourseMapper;
import com.university.erp.course.mapper.SubjectMapper;
import com.university.erp.course.repository.CourseRepository;
import com.university.erp.course.repository.SubjectRepository;
import com.university.erp.department.repository.DepartmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;

    public CourseServiceImpl(CourseRepository courseRepository, SubjectRepository subjectRepository,
            DepartmentRepository departmentRepository) {
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public CourseResponse create(CourseRequest request) {
        validate(request);
        String code = request.courseCode().trim();
        if (courseRepository.existsByCourseCode(code)) {
            throw conflict("Course with code '" + code + "' already exists");
        }
        validateDepartment(request.departmentId());

        Course course = CourseMapper.toEntity(request);
        course.setCourseCode(code);
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse get(Long id) {
        return CourseMapper.toResponse(findCourse(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getByCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw badRequest("courseCode must not be blank");
        }
        Course course = courseRepository.findByCourseCode(courseCode.trim())
                .orElseThrow(() -> notFound("Course not found with code: " + courseCode));
        return CourseMapper.toResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> list(Integer departmentId, String courseType) {
        List<Course> courses;
        if (departmentId != null && courseType != null && !courseType.trim().isEmpty()) {
            courses = courseRepository.findByDepartmentIdAndCourseType(departmentId, courseType.trim());
        } else if (departmentId != null) {
            courses = courseRepository.findByDepartmentId(departmentId);
        } else if (courseType != null && !courseType.trim().isEmpty()) {
            courses = courseRepository.findByCourseType(courseType.trim());
        } else {
            courses = courseRepository.findAll();
        }
        return courses.stream().map(CourseMapper::toResponse).toList();
    }

    @Override
    public CourseResponse update(Long id, CourseRequest request) {
        validate(request);
        Course course = findCourse(id);
        String code = request.courseCode().trim();

        if (courseRepository.existsByCourseCodeAndIdNot(code, id)) {
            throw conflict("Course with code '" + code + "' already exists");
        }
        validateDepartment(request.departmentId());

        CourseMapper.apply(request, course);
        course.setCourseCode(code);
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public void delete(Long id) {
        Course course = findCourse(id);
        if (subjectRepository.existsByCourseId(id)) {
            throw conflict("Cannot delete course with id " + id + " because it contains associated subjects");
        }
        try {
            courseRepository.delete(course);
        } catch (DataIntegrityViolationException ex) {
            throw conflict("Course is referenced by another resource");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsByCourseId(Long courseId) {
        findCourse(courseId);
        return subjectRepository.findByCourseId(courseId).stream().map(SubjectMapper::toResponse).toList();
    }

    private Course findCourse(Long id) {
        if (id == null) {
            throw badRequest("Course ID must not be null");
        }
        return courseRepository.findById(id).orElseThrow(() -> notFound("Course not found with id: " + id));
    }

    private void validateDepartment(Integer departmentId) {
        if (departmentId != null && !departmentRepository.existsById(departmentId)) {
            throw notFound("Department not found with id: " + departmentId);
        }
    }

    private void validate(CourseRequest request) {
        if (request == null) {
            throw badRequest("Course request body must not be null");
        }
        if (request.courseCode() == null || request.courseCode().trim().isEmpty()) {
            throw badRequest("courseCode is required");
        }
        if (request.courseName() == null || request.courseName().trim().isEmpty()) {
            throw badRequest("courseName is required");
        }
        if (request.courseType() == null || request.courseType().trim().isEmpty()) {
            throw badRequest("courseType is required");
        }
        if (request.courseDuration() == null || request.courseDuration() <= 0) {
            throw badRequest("courseDuration must be greater than 0");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
