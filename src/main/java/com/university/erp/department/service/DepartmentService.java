package com.university.erp.department.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.university.erp.department.dto.DepartmentRequest;
import com.university.erp.department.dto.DepartmentResponse;
import com.university.erp.department.entity.Department;
import com.university.erp.department.mapper.DepartmentMapper;
import com.university.erp.department.repository.DepartmentRepository;
import com.university.erp.department.repository.FacultyRepository;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final FacultyRepository facultyRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
            DepartmentMapper departmentMapper,
            FacultyRepository facultyRepository) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
        this.facultyRepository = facultyRepository;
    }

    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByDepartmentNameIgnoreCase(request.departmentName())) {
            throw new IllegalArgumentException("Department name already exists");
        }

        Department department = departmentMapper.toEntity(request);

        return departmentMapper.toResponse(
                departmentRepository.save(department));
    }

    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    public DepartmentResponse getById(Integer id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));

        return departmentMapper.toResponse(department);
    }

    public DepartmentResponse update(Integer id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));

        if (!department.getDepartmentName()
                .equalsIgnoreCase(request.departmentName())
                && departmentRepository
                        .existsByDepartmentNameIgnoreCase(request.departmentName())) {

            throw new IllegalArgumentException(
                    "Department name already exists");
        }

        department.setDepartmentName(request.departmentName());
        department.setLocation(request.location());

        return departmentMapper.toResponse(
                departmentRepository.save(department));
    }

    public void delete(Integer id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));

        if (facultyRepository.existsByDepartmentDepartmentId(id)) {
            throw new IllegalStateException(
                    "Cannot delete department while faculties are assigned to it");
        }

        departmentRepository.delete(department);
    }

    public DepartmentResponse assignHod(Integer departmentId, Integer facultyId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found: " + departmentId));

        var faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found: " + facultyId));

        if (!faculty.getDepartment().getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("Faculty does not belong to the selected department");
        }

        department.setHod(faculty);

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    public DepartmentResponse getHod(Integer departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found: " + departmentId));

        return departmentMapper.toResponse(department);
    }

    public DepartmentResponse removeHod(Integer departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found " + departmentId));

        department.setHod(null);

        return departmentMapper.toResponse(departmentRepository.save(department));

    }
}