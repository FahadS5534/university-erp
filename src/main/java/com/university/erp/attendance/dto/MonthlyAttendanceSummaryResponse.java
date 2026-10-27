package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Aggregated monthly attendance summary for a student")
public record MonthlyAttendanceSummaryResponse(
        @Schema(description = "UUID of the student", example = "8e8ef61b-5621-4c6f-9daa-89c029092e4c") UUID studentId,

        @Schema(description = "Student full name", example = "Alice Smith") String studentName,

        @Schema(description = "Student enrollment number", example = "EN1001") String enrollmentNo,

        @Schema(description = "Calendar year", example = "2026") int year,

        @Schema(description = "Calendar month number (1-12)", example = "10") int month,

        @Schema(description = "Calendar month name", example = "OCTOBER") String monthName,

        @Schema(description = "Total number of marked conducted lectures in the month", example = "36") long totalMarkedLectures,

        @Schema(description = "Number of lectures marked Present", example = "30") long presentCount,

        @Schema(description = "Number of lectures marked Absent", example = "4") long absentCount,

        @Schema(description = "Number of lectures marked C-Work", example = "2") long cWorkCount,

        @Schema(description = "Monthly attendance percentage: ((present + cWork) / total) * 100", example = "88.89") double attendancePercentage,

        @Schema(description = "Total number of distinct days with attendance records in the month", example = "18") int daysWithAttendance) {
}
