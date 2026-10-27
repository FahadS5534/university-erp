package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Chronological attendance history report for a student")
public record StudentAttendanceHistoryResponse(
        @Schema(description = "Student UUID", example = "8e8ef61b-5621-4c6f-9daa-89c029092e4c") UUID studentId,

        @Schema(description = "Enrollment number", example = "EN2026CS001") String enrollmentNo,

        @Schema(description = "Student full name", example = "Jane Doe") String studentName,

        @Schema(description = "Roll number", example = "105") Integer rollNo,

        @Schema(description = "Division", example = "B") String division,

        @Schema(description = "Total eligible conducted lectures evaluated", example = "30") long totalLectures,

        @Schema(description = "Total lectures attended", example = "27") long attendedLectures,

        @Schema(description = "Calculated attendance percentage", example = "90.0") double overallPercentage,

        @Schema(description = "Chronological list of attendance history items") List<AttendanceHistoryRecord> records) {
}
