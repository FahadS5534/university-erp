package com.university.erp.department.controller;

import com.university.erp.department.dto.FacultyRequest;
import com.university.erp.department.dto.FacultyResponse;
import com.university.erp.department.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculties")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyResponse create(@Valid @RequestBody FacultyRequest request) {
        return facultyService.create(request);
    }

    @GetMapping
    public List<FacultyResponse> getAll() {
        return facultyService.getAll();
    }

    @GetMapping("/{id}")
    public FacultyResponse getById(@PathVariable Integer id) {
        return facultyService.getById(id);
    }

    @GetMapping("/search/name")
    public List<FacultyResponse> searchByName(
            @RequestParam String keyword) {

        return facultyService.searchByName(keyword);
    }

    @GetMapping("/search/designation")
    public List<FacultyResponse> searchByDesignation(
            @RequestParam String value) {

        return facultyService.searchByDesignation(value);
    }

    @GetMapping("/search/specialization")
    public List<FacultyResponse> searchBySpecialization(
            @RequestParam String value) {

        return facultyService.searchBySpecialization(value);
    }

    @GetMapping("/department/{departmentId}")
    public List<FacultyResponse> getByDepartment(
            @PathVariable Integer departmentId) {

        return facultyService.getByDepartment(departmentId);
    }

    @PutMapping("/{id}")
    public FacultyResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody FacultyRequest request) {
        return facultyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        facultyService.delete(id);
    }
}
