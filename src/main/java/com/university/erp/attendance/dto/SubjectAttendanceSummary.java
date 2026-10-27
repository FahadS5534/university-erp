package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of attendance for a student in a specific subject")
public record SubjectAttendanceSummary(@Schema(description = "Subject ID", example = "10") Long subjectId,

        @Schema(description = "Subject Code", example = "CS101") String subjectCode,

        @Schema(description = "Subject Name", example = "Data Structures") String subjectName,

        @Schema(description = "Total eligible conducted lectures for the subject", example = "20") long totalLectures,

        @Schema(description = "Count of lectures marked Present", example = "16") long presentCount,

        @Schema(description = "Count of lectures marked Absent", example = "3") long absentCount,

        @Schema(description = "Count of lectures marked C-Work", example = "1") long cWorkCount,

        @Schema(description = "Attendance percentage: ((present + cWork) / total) * 100", example = "85.0") double percentage) {
}
