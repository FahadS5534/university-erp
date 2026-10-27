package com.university.erp.attendance.dto;

import com.university.erp.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Attendance creation or update payload")
public record AttendanceRequest(
        @Schema(description = "Unique ID of the conducted lecture", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "lectureId is required") @Positive(message = "lectureId must be a positive number") Long lectureId,

        @Schema(description = "UUID of the enrolled student", example = "8e8ef61b-5621-4c6f-9daa-89c029092e4c", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "studentId is required") UUID studentId,

        @Schema(description = "Attendance status: Present, Absent, or C_WORK", example = "Present", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "status is required") AttendanceStatus status,

        @Schema(description = "Optional remarks or notes about student attendance", example = "Arrived on time") @Size(max = 500, message = "remarks must not exceed 500 characters") String remarks,

        @Schema(description = "Timestamp when attendance was recorded (defaults to current time if omitted)") LocalDateTime markedAt,

        @Schema(description = "User identifier or faculty username marking the attendance", example = "faculty@university.edu") String markedBy) {
}
