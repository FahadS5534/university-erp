package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Overall attendance trend report with subject-wise and monthly breakdowns")
public record AttendanceTrendsResponse(
        @Schema(description = "UUID of the student", example = "8e8ef61b-5621-4c6f-9daa-89c029092e4c") UUID studentId,

        @Schema(description = "Student full name", example = "Alice Smith") String studentName,

        @Schema(description = "Student enrollment number", example = "EN1001") String enrollmentNo,

        @Schema(description = "Start date of the evaluated trend range", example = "2026-08-01") LocalDate startDate,

        @Schema(description = "End date of the evaluated trend range", example = "2026-10-31") LocalDate endDate,

        @Schema(description = "Total eligible marked lectures evaluated across all subjects", example = "90") long overallTotalLectures,

        @Schema(description = "Total eligible attended lectures (Present + C-Work)", example = "78") long overallAttendedLectures,

        @Schema(description = "Overall percentage across all subjects: (overallAttended / overallTotal) * 100", example = "86.67") double overallPercentage,

        @Schema(description = "Subject-wise attendance metrics and trend breakdown") List<SubjectAttendanceTrend> subjects) {
}
