package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Details of a student whose attendance is below the institutional threshold")
public record AttendanceDefaulterResponse(
        @Schema(description = "Student UUID", example = "8e8ef61b-5621-4c6f-9daa-89c029092e4c") UUID studentId,

        @Schema(description = "Student enrollment number", example = "EN2026CS001") String enrollmentNo,

        @Schema(description = "Full name of the student", example = "John Doe") String studentName,

        @Schema(description = "Roll number in the class", example = "101") Integer rollNo,

        @Schema(description = "Division / section", example = "A") String division,

        @Schema(description = "Total eligible conducted lectures evaluated", example = "40") long totalLectures,

        @Schema(description = "Total eligible lectures attended (Present + C-Work)", example = "24") long attendedLectures,

        @Schema(description = "Calculated attendance percentage", example = "60.0") double percentage,

        @Schema(description = "Defaulter status classification", example = "Critical") String status) {
}
