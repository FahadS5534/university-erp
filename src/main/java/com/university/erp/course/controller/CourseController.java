package com.university.erp.course.controller;

import com.university.erp.course.dto.CourseRequest;
import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/code/{code}")
    public CourseResponse getByCode(@PathVariable String code) {
        return service.getByCourseCode(code);
    }

    @GetMapping
    public List<CourseResponse> list(@RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String courseType) {
        return service.list(departmentId, courseType);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @RequestBody CourseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/subjects")
    public List<SubjectResponse> getSubjects(@PathVariable Long id) {
        return service.getSubjectsByCourseId(id);
    }
}
