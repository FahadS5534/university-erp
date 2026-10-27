package com.university.erp.attendance.controller;

import com.university.erp.attendance.dto.*;
import com.university.erp.attendance.entity.AttendanceStatus;
import com.university.erp.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Attendance Management")
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @Operation(summary = "Mark attendance")
    @PostMapping
    public ResponseEntity<AttendanceResponse> create(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Get attendance by ID")
    @GetMapping("/{id}")
    public AttendanceResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "List and filter attendance records")
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Long lectureId,
            @RequestParam(required = false) UUID studentId, @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String division,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "id,desc") String sort) {

        if (page != null || size != null) {
            int pageIndex = page != null ? Math.max(0, page) : 0;
            int pageSize = size != null ? Math.max(1, size) : 20;

            Sort sorting = Sort.by(Sort.Direction.DESC, "id");
            if (sort != null && !sort.isBlank()) {
                String[] parts = sort.split(",");
                String property = parts[0].trim();
                Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
                sorting = Sort.by(direction, property);
            }

            Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);
            Page<AttendanceResponse> pagedResult = service.listPaged(lectureId, studentId, status, subjectId, division,
                    startDate, endDate, pageable);
            return ResponseEntity.ok(PagedResponse.from(pagedResult));
        }

        List<AttendanceResponse> list = service.list(lectureId, studentId, status, subjectId, division, startDate,
                endDate);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Update attendance record")
    @PutMapping("/{id}")
    public AttendanceResponse update(@PathVariable Long id, @Valid @RequestBody AttendanceRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete attendance record")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Mark attendance in bulk")
    @PostMapping("/bulk")
    public List<AttendanceResponse> markBulk(@RequestBody List<@Valid AttendanceRequest> requests) {
        return service.markBulk(requests);
    }

    @Operation(summary = "Get student attendance summary")
    @GetMapping("/summary/{studentId}")
    public AttendanceSummary summary(@PathVariable UUID studentId, @RequestParam(required = false) Long lectureId) {
        return service.summary(studentId, lectureId);
    }

    @Operation(summary = "Get subject-wise attendance breakdown")
    @GetMapping("/summary/{studentId}/subjects")
    public List<SubjectAttendanceSummary> getSubjectSummaries(@PathVariable UUID studentId) {
        return service.getSubjectSummaries(studentId);
    }

    @Operation(summary = "Get lecture attendance register")
    @GetMapping("/lecture/{lectureId}/register")
    public LectureAttendanceRegister getLectureRegister(@PathVariable Long lectureId) {
        return service.getLectureRegister(lectureId);
    }

    @Operation(summary = "Get student attendance history")
    @GetMapping("/student/{studentId}/history")
    public StudentAttendanceHistoryResponse getStudentHistory(@PathVariable UUID studentId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getStudentHistory(studentId, subjectId, startDate, endDate);
    }

    @Operation(summary = "List attendance defaulters")
    @GetMapping("/defaulters")
    public List<AttendanceDefaulterResponse> getDefaulters(
            @RequestParam(required = false, defaultValue = "75.0") Double threshold,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String division) {
        return service.getDefaulters(threshold, subjectId, division);
    }

    @Operation(summary = "Get monthly attendance calendar")
    @GetMapping("/calendar/{studentId}")
    public ResponseEntity<MonthlyAttendanceCalendarResponse> getMonthlyCalendar(@PathVariable UUID studentId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(service.getMonthlyCalendar(studentId, year, month));
    }

    @Operation(summary = "Get monthly attendance summary")
    @GetMapping("/calendar/{studentId}/summary")
    public ResponseEntity<MonthlyAttendanceSummaryResponse> getMonthlySummary(@PathVariable UUID studentId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(service.getMonthlySummary(studentId, year, month));
    }

    @Operation(summary = "Get subject-wise attendance trends")
    @GetMapping("/calendar/{studentId}/trends")
    public ResponseEntity<AttendanceTrendsResponse> getAttendanceTrends(@PathVariable UUID studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getAttendanceTrends(studentId, startDate, endDate));
    }
}
