package com.university.erp.department.controller;

import com.university.erp.department.dto.FacultyRequest;
import com.university.erp.department.dto.FacultyResponse;
import com.university.erp.department.service.FacultyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculties")
@Tag(
    name = "Faculty Management",
    description = "APIs for creating, retrieving, updating, deleting, and searching university faculty members."
)
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @Operation(
        summary = "Create a faculty member",
        description = "Creates a new faculty member using the provided personal details, "
                    + "professional information, and department ID. The email address must be unique."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyResponse create(
            @Valid @RequestBody FacultyRequest request) {
        return facultyService.create(request);
    }

    @Operation(
        summary = "Get all faculty members",
        description = "Retrieves a list of all faculty members, including their personal details, "
                    + "designation, qualification, specialization, and department information."
    )
    @GetMapping
    public List<FacultyResponse> getAll() {
        return facultyService.getAll();
    }

    @Operation(
        summary = "Get a faculty member by ID",
        description = "Retrieves the details of a specific faculty member using their unique faculty ID. "
                    + "Returns an error if the faculty member does not exist."
    )
    @GetMapping("/{id}")
    public FacultyResponse getById(@PathVariable Integer id) {
        return facultyService.getById(id);
    }

    @Operation(
        summary = "Search faculty by name",
        description = "Searches faculty members by first name or last name using a keyword. "
                    + "The search is case-insensitive. Provide the search keyword as a query parameter."
    )
    @GetMapping("/search/name")
    public List<FacultyResponse> searchByName(
            @RequestParam String keyword) {
        return facultyService.searchByName(keyword);
    }

    @Operation(
        summary = "Search faculty by designation",
        description = "Retrieves faculty members whose designation contains the specified value, "
                    + "ignoring letter case. Provide the designation value as a query parameter."
    )
    @GetMapping("/search/designation")
    public List<FacultyResponse> searchByDesignation(
            @RequestParam String value) {
        return facultyService.searchByDesignation(value);
    }

    @Operation(
        summary = "Search faculty by specialization",
        description = "Retrieves faculty members whose specialization contains the specified value, "
                    + "ignoring letter case. Provide the specialization value as a query parameter."
    )
    @GetMapping("/search/specialization")
    public List<FacultyResponse> searchBySpecialization(
            @RequestParam String value) {
        return facultyService.searchBySpecialization(value);
    }

    @Operation(
        summary = "Get faculty by department",
        description = "Retrieves all faculty members assigned to the specified department ID. "
                    + "Provide the department ID in the URL."
    )
    @GetMapping("/department/{departmentId}")
    public List<FacultyResponse> getByDepartment(
            @PathVariable Integer departmentId) {
        return facultyService.getByDepartment(departmentId);
    }

    @Operation(
        summary = "Update a faculty member",
        description = "Updates an existing faculty member's personal details, professional information, "
                    + "and department assignment using their faculty ID. The email address must not "
                    + "belong to another faculty member."
    )
    @PutMapping("/{id}")
    public FacultyResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody FacultyRequest request) {
        return facultyService.update(id, request);
    }

    @Operation(
        summary = "Delete a faculty member",
        description = "Deletes a faculty member using their unique faculty ID. "
                    + "Deletion may be rejected if the faculty member is still assigned as a "
                    + "Head of Department."
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        facultyService.delete(id);
    }
}

