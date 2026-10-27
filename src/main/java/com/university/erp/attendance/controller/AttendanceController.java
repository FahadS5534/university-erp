package com.university.erp.attendance.controller;

import com.university.erp.attendance.dto.*;
import com.university.erp.attendance.entity.AttendanceStatus;
import com.university.erp.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@Tag(name = "Attendance Management", description = "Endpoints for marking, updating, filtering, and reporting attendance")
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @Operation(summary = "Mark a single attendance record", description = "Records attendance for a single student in a conducted lecture session.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Attendance record created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing required fields"),
            @ApiResponse(responseCode = "404", description = "Referenced lecture or student does not exist"),
            @ApiResponse(responseCode = "409", description = "Attendance record already exists for this student and lecture") })
    @PostMapping
    public ResponseEntity<AttendanceResponse> create(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Get attendance record by ID", description = "Retrieves an attendance entry by its unique primary key ID.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Attendance record found"),
            @ApiResponse(responseCode = "404", description = "Attendance record not found") })
    @GetMapping("/{id}")
    public AttendanceResponse get(@Parameter(description = "Attendance record ID") @PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "List and filter attendance records", description = "Retrieves attendance records with optional filtering by lecture, student, status, subject, division, and date range. Supports pagination.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "List or paged result of attendance records") })
    @GetMapping
    public ResponseEntity<?> list(
            @Parameter(description = "Filter by lecture ID") @RequestParam(required = false) Long lectureId,
            @Parameter(description = "Filter by student UUID") @RequestParam(required = false) UUID studentId,
            @Parameter(description = "Filter by attendance status (Present, Absent, C_WORK)") @RequestParam(required = false) AttendanceStatus status,
            @Parameter(description = "Filter by subject ID") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "Filter by student division") @RequestParam(required = false) String division,
            @Parameter(description = "Filter by start date (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter by end date (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number (0-based) for pagination") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer size,
            @Parameter(description = "Sorting criteria (e.g. id,desc or markedAt,asc)") @RequestParam(required = false, defaultValue = "id,desc") String sort) {

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

    @Operation(summary = "Update an existing attendance record", description = "Updates status, remarks, or metadata for an existing attendance entry.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Attendance record updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "404", description = "Attendance record or referenced entities not found"),
            @ApiResponse(responseCode = "409", description = "Updated lecture and student combination conflicts with existing record") })
    @PutMapping("/{id}")
    public AttendanceResponse update(@Parameter(description = "Attendance record ID to update") @PathVariable Long id,
            @Valid @RequestBody AttendanceRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete an attendance record", description = "Deletes an attendance record by its ID if permitted by business rules.")
    @ApiResponses({ @ApiResponse(responseCode = "204", description = "Attendance record deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Record cannot be deleted due to policy constraint"),
            @ApiResponse(responseCode = "404", description = "Attendance record not found") })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Attendance record ID to delete") @PathVariable Long id) {
        service.delete(id);
    }

    @Operation(summary = "Mark attendance in bulk", description = "Marks or updates attendance for multiple students atomically in a single request.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Bulk attendance marked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input, empty payload, or duplicate items in batch"),
            @ApiResponse(responseCode = "404", description = "Referenced lecture or student not found") })
    @PostMapping("/bulk")
    public List<AttendanceResponse> markBulk(@RequestBody List<@Valid AttendanceRequest> requests) {
        return service.markBulk(requests);
    }

    @Operation(summary = "Get overall attendance summary for a student", description = "Calculates total lectures, present, absent, and percentage for a student, optionally filtered by lecture.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Attendance summary retrieved"),
            @ApiResponse(responseCode = "404", description = "Student or lecture not found") })
    @GetMapping("/summary/{studentId}")
    public AttendanceSummary summary(@Parameter(description = "Student UUID") @PathVariable UUID studentId,
            @Parameter(description = "Optional lecture ID to filter summary") @RequestParam(required = false) Long lectureId) {
        return service.summary(studentId, lectureId);
    }

    @Operation(summary = "Get subject-wise attendance breakdown", description = "Retrieves attendance counts and percentage breakdown by subject for a given student.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Subject summaries retrieved"),
            @ApiResponse(responseCode = "404", description = "Student not found") })
    @GetMapping("/summary/{studentId}/subjects")
    public List<SubjectAttendanceSummary> getSubjectSummaries(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId) {
        return service.getSubjectSummaries(studentId);
    }

    @Operation(summary = "Get lecture attendance register", description = "Returns full attendance register for a specific lecture session with summary statistics and student records.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Lecture register retrieved"),
            @ApiResponse(responseCode = "404", description = "Lecture not found") })
    @GetMapping("/lecture/{lectureId}/register")
    public LectureAttendanceRegister getLectureRegister(
            @Parameter(description = "Lecture ID") @PathVariable Long lectureId) {
        return service.getLectureRegister(lectureId);
    }

    @Operation(summary = "Get student attendance history", description = "Retrieves full chronological attendance history for a student with optional subject and date range filters.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Student attendance history retrieved"),
            @ApiResponse(responseCode = "404", description = "Student not found") })
    @GetMapping("/student/{studentId}/history")
    public StudentAttendanceHistoryResponse getStudentHistory(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            @Parameter(description = "Optional subject ID filter") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "Optional start date filter (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Optional end date filter (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getStudentHistory(studentId, subjectId, startDate, endDate);
    }

    @Operation(summary = "Identify attendance defaulters", description = "Lists students whose attendance percentage falls below a configured threshold (default 75%). Supports filtering by subject and division.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Defaulters list retrieved") })
    @GetMapping("/defaulters")
    public List<AttendanceDefaulterResponse> getDefaulters(
            @Parameter(description = "Minimum required attendance percentage (e.g. 75.0)") @RequestParam(required = false, defaultValue = "75.0") Double threshold,
            @Parameter(description = "Optional subject ID filter") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "Optional division filter (e.g. A, B)") @RequestParam(required = false) String division) {
        return service.getDefaulters(threshold, subjectId, division);
    }

    @Operation(summary = "Get monthly attendance calendar", description = "Returns full day-by-day attendance calendar for a student and specified calendar month, including lecture details, statuses, and monthly totals.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly calendar successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid year or month specified"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/calendar/{studentId}")
    public ResponseEntity<MonthlyAttendanceCalendarResponse> getMonthlyCalendar(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            @Parameter(description = "Calendar year (e.g. 2026)") @RequestParam int year,
            @Parameter(description = "Calendar month number (1-12)") @RequestParam int month) {
        return ResponseEntity.ok(service.getMonthlyCalendar(studentId, year, month));
    }

    @Operation(summary = "Get monthly attendance summary", description = "Calculates monthly attendance totals, status counts, percentage, and active days for a student in a specified month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly summary successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid year or month specified"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/calendar/{studentId}/summary")
    public ResponseEntity<MonthlyAttendanceSummaryResponse> getMonthlySummary(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            @Parameter(description = "Calendar year (e.g. 2026)") @RequestParam int year,
            @Parameter(description = "Calendar month number (1-12)") @RequestParam int month) {
        return ResponseEntity.ok(service.getMonthlySummary(studentId, year, month));
    }

    @Operation(summary = "Get subject-wise attendance trends", description = "Analyzes subject-wise attendance metrics and chronological monthly trend breakdowns for a student over the requested date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance trends report successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid date range or missing required date parameters"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/calendar/{studentId}/trends")
    public ResponseEntity<AttendanceTrendsResponse> getAttendanceTrends(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getAttendanceTrends(studentId, startDate, endDate));
    }
}
