package com.university.erp.attendance.controller;

import com.university.erp.attendance.dto.*;
import com.university.erp.attendance.entity.AttendanceStatus;
import com.university.erp.attendance.service.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private AttendanceController attendanceController;

    private AttendanceResponse sampleResponse;
    private UUID sampleStudentId;
    private Long sampleLectureId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attendanceController)
                .setControllerAdvice(new AttendanceExceptionHandler()).build();

        sampleStudentId = UUID.fromString("8e8ef61b-5621-4c6f-9daa-89c029092e4c");
        sampleLectureId = 1L;

        sampleResponse = new AttendanceResponse(100L, sampleLectureId, sampleStudentId, AttendanceStatus.Present,
                "On time", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), "prof@university.edu", null);
    }

    @Test
    void testCreate_Success() throws Exception {
        when(attendanceService.create(any(AttendanceRequest.class))).thenReturn(sampleResponse);

        String json = """
                {
                    "lectureId": 1,
                    "studentId": "8e8ef61b-5621-4c6f-9daa-89c029092e4c",
                    "status": "Present",
                    "remarks": "On time",
                    "markedBy": "prof@university.edu"
                }
                """;

        mockMvc.perform(post("/api/attendance").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.lectureId").value(1L)).andExpect(jsonPath("$.status").value("Present"));
    }

    @Test
    void testCreate_ValidationError() throws Exception {
        String invalidJson = """
                {
                    "lectureId": null,
                    "studentId": null,
                    "status": null
                }
                """;

        mockMvc.perform(post("/api/attendance").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details.lectureId").exists());
    }

    @Test
    void testGet_Success() throws Exception {
        when(attendanceService.get(100L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/attendance/100")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.lectureId").value(1L));
    }

    @Test
    void testList_Unpaged() throws Exception {
        when(attendanceService.list(eq(sampleLectureId), eq(sampleStudentId), eq(AttendanceStatus.Present), isNull(),
                isNull(), isNull(), isNull())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/attendance").param("lectureId", "1").param("studentId", sampleStudentId.toString())
                .param("status", "Present")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(100L));
    }

    @Test
    void testList_Paged() throws Exception {
        Page<AttendanceResponse> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 10), 1);
        when(attendanceService.listPaged(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/attendance").param("page", "0").param("size", "10").param("sort", "id,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }

    @Test
    void testUpdate_Success() throws Exception {
        when(attendanceService.update(eq(100L), any(AttendanceRequest.class))).thenReturn(sampleResponse);

        String json = """
                {
                    "lectureId": 1,
                    "studentId": "8e8ef61b-5621-4c6f-9daa-89c029092e4c",
                    "status": "Present",
                    "remarks": "Updated remark",
                    "markedBy": "prof@university.edu"
                }
                """;

        mockMvc.perform(put("/api/attendance/100").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    void testDelete_Success() throws Exception {
        doNothing().when(attendanceService).delete(100L);

        mockMvc.perform(delete("/api/attendance/100")).andExpect(status().isNoContent());

        verify(attendanceService).delete(100L);
    }

    @Test
    void testMarkBulk_Success() throws Exception {
        when(attendanceService.markBulk(anyList())).thenReturn(List.of(sampleResponse));

        String json = """
                [
                    {
                        "lectureId": 1,
                        "studentId": "8e8ef61b-5621-4c6f-9daa-89c029092e4c",
                        "status": "Present",
                        "markedBy": "prof@university.edu"
                    }
                ]
                """;

        mockMvc.perform(post("/api/attendance/bulk").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(100L));
    }

    @Test
    void testSummary_Success() throws Exception {
        AttendanceSummary summary = new AttendanceSummary(sampleStudentId, 10, 8, 1, 1, 90.0);
        when(attendanceService.summary(sampleStudentId, sampleLectureId)).thenReturn(summary);

        mockMvc.perform(get("/api/attendance/summary/" + sampleStudentId).param("lectureId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.present").value(8)).andExpect(jsonPath("$.percentage").value(90.0));
    }

    @Test
    void testGetSubjectSummaries_Success() throws Exception {
        SubjectAttendanceSummary subjectSummary = new SubjectAttendanceSummary(10L, "CS101", "Operating Systems", 20L,
                16L, 3L, 1L, 85.0);
        when(attendanceService.getSubjectSummaries(sampleStudentId)).thenReturn(List.of(subjectSummary));

        mockMvc.perform(get("/api/attendance/summary/" + sampleStudentId + "/subjects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectCode").value("CS101"))
                .andExpect(jsonPath("$[0].percentage").value(85.0));
    }

    @Test
    void testGetLectureRegister_Success() throws Exception {
        LectureAttendanceRegister register = new LectureAttendanceRegister(1L, 10L, "Operating Systems", 5,
                LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), "Theory", "Concurrency", "Conducted", 1L, 1L,
                0L, 0L, 100.0, List.of(sampleResponse));
        when(attendanceService.getLectureRegister(1L)).thenReturn(register);

        mockMvc.perform(get("/api/attendance/lecture/1/register")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lectureId").value(1L))
                .andExpect(jsonPath("$.subjectName").value("Operating Systems"))
                .andExpect(jsonPath("$.attendanceRate").value(100.0));
    }

    @Test
    void testGetStudentHistory_Success() throws Exception {
        StudentAttendanceHistoryResponse history = new StudentAttendanceHistoryResponse(sampleStudentId, "EN1001",
                "Alice Smith", 101, "A", 10L, 9L, 90.0, List.of());
        when(attendanceService.getStudentHistory(eq(sampleStudentId), isNull(), isNull(), isNull()))
                .thenReturn(history);

        mockMvc.perform(get("/api/attendance/student/" + sampleStudentId + "/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Alice Smith"))
                .andExpect(jsonPath("$.overallPercentage").value(90.0));
    }

    @Test
    void testGetDefaulters_Success() throws Exception {
        AttendanceDefaulterResponse defaulter = new AttendanceDefaulterResponse(sampleStudentId, "EN1001",
                "Alice Smith", 101, "A", 40L, 20L, 50.0, "Warning");
        when(attendanceService.getDefaulters(eq(75.0), isNull(), isNull())).thenReturn(List.of(defaulter));

        mockMvc.perform(get("/api/attendance/defaulters").param("threshold", "75.0")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Alice Smith"))
                .andExpect(jsonPath("$[0].percentage").value(50.0)).andExpect(jsonPath("$[0].status").value("Warning"));
    }

    @Test
    void testGetMonthlyCalendar_Success() throws Exception {
        MonthlyAttendanceCalendarResponse calendarResponse = new MonthlyAttendanceCalendarResponse(
                sampleStudentId, "Alice Smith", "EN1001", 2026, 10, "OCTOBER", 31, 15, 30L, 25L, 4L, 1L, 86.67, List.of()
        );
        when(attendanceService.getMonthlyCalendar(sampleStudentId, 2026, 10)).thenReturn(calendarResponse);

        mockMvc.perform(get("/api/attendance/calendar/" + sampleStudentId)
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(sampleStudentId.toString()))
                .andExpect(jsonPath("$.studentName").value("Alice Smith"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.monthName").value("OCTOBER"))
                .andExpect(jsonPath("$.totalDaysInMonth").value(31))
                .andExpect(jsonPath("$.activeAttendanceDays").value(15))
                .andExpect(jsonPath("$.totalLectures").value(30))
                .andExpect(jsonPath("$.presentCount").value(25))
                .andExpect(jsonPath("$.attendancePercentage").value(86.67));
    }

    @Test
    void testGetMonthlyCalendar_InvalidMonth() throws Exception {
        when(attendanceService.getMonthlyCalendar(sampleStudentId, 2026, 13))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12"));

        mockMvc.perform(get("/api/attendance/calendar/" + sampleStudentId)
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Month must be between 1 and 12"));
    }

    @Test
    void testGetMonthlyCalendar_StudentNotFound() throws Exception {
        when(attendanceService.getMonthlyCalendar(sampleStudentId, 2026, 10))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + sampleStudentId));

        mockMvc.perform(get("/api/attendance/calendar/" + sampleStudentId)
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Student not found: " + sampleStudentId));
    }

    @Test
    void testGetMonthlySummary_Success() throws Exception {
        MonthlyAttendanceSummaryResponse summaryResponse = new MonthlyAttendanceSummaryResponse(
                sampleStudentId, "Alice Smith", "EN1001", 2026, 10, "OCTOBER", 20L, 18L, 1L, 1L, 95.0, 10
        );
        when(attendanceService.getMonthlySummary(sampleStudentId, 2026, 10)).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/attendance/calendar/" + sampleStudentId + "/summary")
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(sampleStudentId.toString()))
                .andExpect(jsonPath("$.studentName").value("Alice Smith"))
                .andExpect(jsonPath("$.totalMarkedLectures").value(20))
                .andExpect(jsonPath("$.presentCount").value(18))
                .andExpect(jsonPath("$.attendancePercentage").value(95.0))
                .andExpect(jsonPath("$.daysWithAttendance").value(10));
    }

    @Test
    void testGetMonthlySummary_InvalidYear() throws Exception {
        when(attendanceService.getMonthlySummary(sampleStudentId, 1800, 10))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year must be between 1900 and 2200"));

        mockMvc.perform(get("/api/attendance/calendar/" + sampleStudentId + "/summary")
                        .param("year", "1800")
                        .param("month", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Year must be between 1900 and 2200"));
    }

    @Test
    void testGetAttendanceTrends_Success() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        SubjectAttendanceTrend subj = new SubjectAttendanceTrend(
                10L, "CS101", "Data Structures", 40L, 35L, 3L, 2L, 92.5, List.of()
        );
        AttendanceTrendsResponse trendsResponse = new AttendanceTrendsResponse(
                sampleStudentId, "Alice Smith", "EN1001", startDate, endDate, 40L, 37L, 92.5, List.of(subj)
        );
        when(attendanceService.getAttendanceTrends(sampleStudentId, startDate, endDate)).thenReturn(trendsResponse);

        mockMvc.perform(get("/api/attendance/calendar/" + sampleStudentId + "/trends")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(sampleStudentId.toString()))
                .andExpect(jsonPath("$.studentName").value("Alice Smith"))
                .andExpect(jsonPath("$.overallTotalLectures").value(40))
                .andExpect(jsonPath("$.overallAttendedLectures").value(37))
                .andExpect(jsonPath("$.overallPercentage").value(92.5))
                .andExpect(jsonPath("$.subjects[0].subjectName").value("Data Structures"))
                .andExpect(jsonPath("$.subjects[0].attendancePercentage").value(92.5));
    }

    @Test
    void testGetAttendanceTrends_InvalidDateRange() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 10, 31);
        LocalDate endDate = LocalDate.of(2026, 8, 1);

        when(attendanceService.getAttendanceTrends(sampleStudentId, startDate, endDate))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate"));

        mockMvc.perform(get("/api/attendance/calendar/" + sampleStudentId + "/trends")
                        .param("startDate", "2026-10-31")
                        .param("endDate", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("startDate cannot be after endDate"));
    }
}
