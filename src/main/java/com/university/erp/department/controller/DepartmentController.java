package com.university.erp.department.controller;

import com.university.erp.department.dto.DepartmentRequest;
import com.university.erp.department.dto.DepartmentResponse;
import com.university.erp.department.service.DepartmentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Department Management", description = "Manage university departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation( summary = "Create a department", description = "Creates a new department using the provided department name and location. " + "The department name must be unique. Returns the created department." )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        return departmentService.create(request);
    }

    @Operation( summary = "Get all departments", description = "Retrieves a list of all departments registered in the university ERP system." )
    @GetMapping
    public List<DepartmentResponse> getAll() {
        return departmentService.getAll();
    }

   @Operation( summary = "Get a department by ID", description = "Retrieves the details of a department using its unique department ID. " + "Returns an error if the department does not exist." )
    @GetMapping("/{id}")
    public DepartmentResponse getById(@PathVariable Integer id) {
        return departmentService.getById(id);
    }

    @Operation( summary = "Update a department", description = "Updates the name and location of an existing department. " + "The department name must remain unique. Returns the updated department." )
    @PutMapping("/{id}")
    public DepartmentResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(id, request);
    }

   @Operation( summary = "Delete a department", description = "Deletes a department using its department ID. " + "Deletion may be rejected if faculty members are assigned to the department." )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        departmentService.delete(id);
    }

   @Operation( summary = "Assign a Head of Department", description = "Assigns a faculty member as the HOD of the specified department " + "using the department ID and faculty ID." )
    @PutMapping("/{departmentId}/hod/{facultyId}")
    public DepartmentResponse assignHod(
        @PathVariable Integer departmentId,
        @PathVariable Integer facultyId){

            return  departmentService.assignHod(departmentId, facultyId);
        }

    @Operation( summary = "Get the Head of Department", description = "Retrieves the department information and its assigned HOD " + "using the department ID." )   
    @GetMapping ("/{departmentId}/hod")
    public DepartmentResponse getHod(
        @PathVariable Integer departmentId){

            return departmentService.getHod(departmentId);
        }
    
    @Operation( summary = "Remove the Head of Department", description = "Removes the HOD assignment from the specified department " + "without deleting the faculty member." )
    @DeleteMapping ("/{departmentId}/hod")
    public DepartmentResponse removeHod(
        @PathVariable Integer departmentId){

            return departmentService.removeHod(departmentId);
        }
}
