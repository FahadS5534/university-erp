package com.university.erp.attendance.dto;

import com.university.erp.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Individual attendance entry for a student on a specific lecture session")
public record DailyAttendanceItem(@Schema(description = "Attendance record ID", example = "101") Long attendanceId,

        @Schema(description = "Conducted lecture ID", example = "1") Long lectureId,

        @Schema(description = "Scheduled lecture date", example = "2026-10-15") LocalDate lectureDate,

        @Schema(description = "Lecture scheduled start time", example = "09:00:00") LocalTime startTime,

        @Schema(description = "Lecture scheduled end time", example = "10:00:00") LocalTime endTime,

        @Schema(description = "Subject ID", example = "10") Long subjectId,

        @Schema(description = "Subject code", example = "CS101") String subjectCode,

        @Schema(description = "Subject name", example = "Data Structures") String subjectName,

        @Schema(description = "Attendance status: Present, Absent, or C_WORK", example = "Present") AttendanceStatus status,

        @Schema(description = "Remarks or notes", example = "Attended on time") String remarks,

        @Schema(description = "Timestamp when attendance was marked") LocalDateTime markedAt,

        @Schema(description = "User or faculty identifier who marked attendance", example = "faculty@university.edu") String markedBy) {
}
