package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Full monthly calendar response containing day-by-day attendance records and monthly totals")
public record MonthlyAttendanceCalendarResponse(
        @Schema(description = "UUID of the student", example = "8e8ef61b-5621-4c6f-9daa-89c029092e4c") UUID studentId,

        @Schema(description = "Student full name", example = "Alice Smith") String studentName,

        @Schema(description = "Student enrollment number", example = "EN1001") String enrollmentNo,

        @Schema(description = "Calendar year", example = "2026") int year,

        @Schema(description = "Calendar month number (1-12)", example = "10") int month,

        @Schema(description = "Calendar month name", example = "OCTOBER") String monthName,

        @Schema(description = "Total number of calendar days in this month", example = "31") int totalDaysInMonth,

        @Schema(description = "Number of days in this month on which attendance was recorded", example = "18") int activeAttendanceDays,

        @Schema(description = "Total lectures marked across the month", example = "36") long totalLectures,

        @Schema(description = "Total lectures marked Present", example = "30") long presentCount,

        @Schema(description = "Total lectures marked Absent", example = "4") long absentCount,

        @Schema(description = "Total lectures marked C-Work", example = "2") long cWorkCount,

        @Schema(description = "Monthly attendance percentage: ((present + cWork) / total) * 100", example = "88.89") double attendancePercentage,

        @Schema(description = "List of all days in the calendar month with associated attendance records") List<CalendarDayAttendance> days) {
}
