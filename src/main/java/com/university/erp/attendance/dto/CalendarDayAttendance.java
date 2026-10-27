package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Schema(description = "Attendance record grouping for a specific calendar day")
public record CalendarDayAttendance(@Schema(description = "Calendar date", example = "2026-10-15") LocalDate date,

        @Schema(description = "Day of the month (1-31)", example = "15") int dayOfMonth,

        @Schema(description = "Name of the day of the week", example = "THURSDAY") String dayOfWeek,

        @Schema(description = "Total lectures scheduled/marked on this day", example = "2") int totalLectures,

        @Schema(description = "Number of lectures marked Present", example = "1") int presentCount,

        @Schema(description = "Number of lectures marked Absent", example = "0") int absentCount,

        @Schema(description = "Number of lectures marked C-Work", example = "1") int cWorkCount,

        @Schema(description = "List of attendance entries for this day") List<DailyAttendanceItem> entries) {
    public static CalendarDayAttendance empty(LocalDate date) {
        return new CalendarDayAttendance(date, date.getDayOfMonth(), date.getDayOfWeek().name(), 0, 0, 0, 0,
                Collections.emptyList());
    }
}
