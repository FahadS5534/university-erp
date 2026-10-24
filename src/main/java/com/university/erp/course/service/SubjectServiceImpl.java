package com.university.erp.course.service;

import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectRequest;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.entity.Course;
import com.university.erp.course.entity.Subject;
import com.university.erp.course.mapper.CourseMapper;
import com.university.erp.course.mapper.SubjectMapper;
import com.university.erp.course.repository.CourseRepository;
import com.university.erp.course.repository.SubjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;

    public SubjectServiceImpl(SubjectRepository subjectRepository, CourseRepository courseRepository) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public SubjectResponse create(SubjectRequest request) {
        validate(request);
        validateCourseExists(request.courseId());

        String code = request.subjectCode().trim();
        if (subjectRepository.existsBySubjectCode(code)) {
            throw conflict("Subject with code '" + code + "' already exists");
        }

        Subject subject = SubjectMapper.toEntity(request);
        subject.setSubjectCode(code);
        return SubjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse get(Long id) {
        return SubjectMapper.toResponse(findSubject(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getBySubjectCode(String subjectCode) {
        if (subjectCode == null || subjectCode.trim().isEmpty()) {
            throw badRequest("subjectCode must not be blank");
        }
        Subject subject = subjectRepository.findBySubjectCode(subjectCode.trim())
                .orElseThrow(() -> notFound("Subject not found with code: " + subjectCode));
        return SubjectMapper.toResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> list(Long courseId, Short inSemester, String subjectType) {
        List<Subject> subjects;
        if (courseId != null && inSemester != null) {
            subjects = subjectRepository.findByCourseIdAndInSemester(courseId, inSemester);
        } else if (courseId != null) {
            subjects = subjectRepository.findByCourseId(courseId);
        } else if (subjectType != null && !subjectType.trim().isEmpty()) {
            subjects = subjectRepository.findBySubjectType(subjectType.trim());
        } else {
            subjects = subjectRepository.findAll();
        }

        return subjects.stream().filter(s -> courseId == null || s.getCourseId().equals(courseId))
                .filter(s -> inSemester == null || (s.getInSemester() != null && s.getInSemester().equals(inSemester)))
                .filter(s -> subjectType == null || subjectType.trim().isEmpty()
                        || (s.getSubjectType() != null && s.getSubjectType().equalsIgnoreCase(subjectType.trim())))
                .map(SubjectMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseBySubjectId(Long id) {
        Subject subject = findSubject(id);
        Course course = courseRepository.findById(subject.getCourseId())
                .orElseThrow(() -> notFound("Course not found with id: " + subject.getCourseId()));
        return CourseMapper.toResponse(course);
    }

    @Override
    public SubjectResponse update(Long id, SubjectRequest request) {
        validate(request);
        Subject subject = findSubject(id);
        validateCourseExists(request.courseId());

        String code = request.subjectCode().trim();
        if (subjectRepository.existsBySubjectCodeAndIdNot(code, id)) {
            throw conflict("Subject with code '" + code + "' already exists");
        }

        SubjectMapper.apply(request, subject);
        subject.setSubjectCode(code);
        return SubjectMapper.toResponse(subjectRepository.save(subject));
    }

    @Override
    public void delete(Long id) {
        Subject subject = findSubject(id);
        try {
            subjectRepository.delete(subject);
        } catch (DataIntegrityViolationException ex) {
            throw conflict("Subject is referenced by another resource");
        }
    }

    private Subject findSubject(Long id) {
        if (id == null) {
            throw badRequest("Subject ID must not be null");
        }
        return subjectRepository.findById(id).orElseThrow(() -> notFound("Subject not found with id: " + id));
    }

    private void validateCourseExists(Long courseId) {
        if (courseId == null || !courseRepository.existsById(courseId)) {
            throw notFound("Course not found with id: " + courseId);
        }
    }

    private void validate(SubjectRequest request) {
        if (request == null) {
            throw badRequest("Subject request body must not be null");
        }
        if (request.subjectCode() == null || request.subjectCode().trim().isEmpty()) {
            throw badRequest("subjectCode is required");
        }
        if (request.subjectName() == null || request.subjectName().trim().isEmpty()) {
            throw badRequest("subjectName is required");
        }
        if (request.courseId() == null) {
            throw badRequest("courseId is required");
        }
        if (request.credits() == null || request.credits() < 0) {
            throw badRequest("credits must be non-negative");
        }
        if (request.subjectType() == null || request.subjectType().trim().isEmpty()) {
            throw badRequest("subjectType is required");
        }
        if (request.subjectHours() == null || request.subjectHours() < 0) {
            throw badRequest("subjectHours must be non-negative");
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
