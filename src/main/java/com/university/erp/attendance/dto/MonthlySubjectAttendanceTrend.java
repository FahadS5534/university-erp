package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Monthly breakdown of attendance for a specific subject")
public record MonthlySubjectAttendanceTrend(@Schema(description = "Calendar year", example = "2026") int year,

        @Schema(description = "Calendar month number (1-12)", example = "10") int month,

        @Schema(description = "Calendar month name", example = "OCTOBER") String monthName,

        @Schema(description = "Total lectures marked in this month for this subject", example = "12") long totalLectures,

        @Schema(description = "Number of lectures marked Present", example = "10") long presentCount,

        @Schema(description = "Number of lectures marked Absent", example = "1") long absentCount,

        @Schema(description = "Number of lectures marked C-Work", example = "1") long cWorkCount,

        @Schema(description = "Attendance percentage for this month", example = "91.67") double percentage) {
}
