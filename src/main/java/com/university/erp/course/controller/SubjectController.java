package com.university.erp.course.controller;

import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectRequest;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> create(@RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public SubjectResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/code/{code}")
    public SubjectResponse getByCode(@PathVariable String code) {
        return service.getBySubjectCode(code);
    }

    @GetMapping
    public List<SubjectResponse> list(@RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Short semester, @RequestParam(required = false) String subjectType) {
        return service.list(courseId, semester, subjectType);
    }

    @GetMapping("/{id}/course")
    public CourseResponse getCourse(@PathVariable Long id) {
        return service.getCourseBySubjectId(id);
    }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @RequestBody SubjectRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
