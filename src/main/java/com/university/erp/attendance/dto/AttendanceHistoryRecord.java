package com.university.erp.attendance.dto;

import com.university.erp.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Individual attendance entry in a student's history")
public record AttendanceHistoryRecord(@Schema(description = "Attendance record ID", example = "101") Long attendanceId,

        @Schema(description = "Lecture ID", example = "1") Long lectureId,

        @Schema(description = "Date of the lecture", example = "2026-10-27") LocalDate lectureDate,

        @Schema(description = "Lecture start time", example = "09:00:00") LocalTime startTime,

        @Schema(description = "Lecture end time", example = "10:00:00") LocalTime endTime,

        @Schema(description = "Subject ID", example = "10") Long subjectId,

        @Schema(description = "Subject Name", example = "Operating Systems") String subjectName,

        @Schema(description = "Attendance status", example = "Present") AttendanceStatus status,

        @Schema(description = "Remarks", example = "On time") String remarks,

        @Schema(description = "Timestamp when marked") LocalDateTime markedAt,

        @Schema(description = "User or faculty who marked the record", example = "faculty@university.edu") String markedBy) {
}
