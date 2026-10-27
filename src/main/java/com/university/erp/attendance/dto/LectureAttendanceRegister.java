package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Complete attendance register for a specific lecture session")
public record LectureAttendanceRegister(@Schema(description = "Lecture ID", example = "1") Long lectureId,

        @Schema(description = "Subject ID", example = "10") Long subjectId,

        @Schema(description = "Subject Name", example = "Operating Systems") String subjectName,

        @Schema(description = "Faculty ID who conducted the lecture", example = "5") Integer facultyId,

        @Schema(description = "Date when lecture took place", example = "2026-10-27") LocalDate lectureDate,

        @Schema(description = "Scheduled start time", example = "09:00:00") LocalTime startTime,

        @Schema(description = "Scheduled end time", example = "10:00:00") LocalTime endTime,

        @Schema(description = "Type of lecture (e.g. Theory, Lab)", example = "Theory") String lectureType,

        @Schema(description = "Topic covered in the lecture", example = "Process Synchronization") String topic,

        @Schema(description = "Status of the lecture", example = "Conducted") String lectureStatus,

        @Schema(description = "Total students marked for this lecture", example = "60") long totalMarked,

        @Schema(description = "Number of students marked Present", example = "52") long presentCount,

        @Schema(description = "Number of students marked Absent", example = "6") long absentCount,

        @Schema(description = "Number of students marked C-Work", example = "2") long cWorkCount,

        @Schema(description = "Overall attendance rate percentage for this session", example = "90.0") double attendanceRate,

        @Schema(description = "Detailed list of student attendance records for this lecture") List<AttendanceResponse> records) {
}
