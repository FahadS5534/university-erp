package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Subject attendance metrics and chronological monthly trend over a date range")
public record SubjectAttendanceTrend(@Schema(description = "Subject ID", example = "10") Long subjectId,

        @Schema(description = "Subject Code", example = "CS101") String subjectCode,

        @Schema(description = "Subject Name", example = "Operating Systems") String subjectName,

        @Schema(description = "Total eligible marked lectures over date range", example = "30") long totalLectures,

        @Schema(description = "Number of lectures marked Present", example = "25") long presentCount,

        @Schema(description = "Number of lectures marked Absent", example = "3") long absentCount,

        @Schema(description = "Number of lectures marked C-Work", example = "2") long cWorkCount,

        @Schema(description = "Overall attendance percentage for this subject: ((present + cWork) / total) * 100", example = "90.0") double attendancePercentage,

        @Schema(description = "Chronological monthly breakdown of attendance for this subject") List<MonthlySubjectAttendanceTrend> monthlyBreakdown) {
}
