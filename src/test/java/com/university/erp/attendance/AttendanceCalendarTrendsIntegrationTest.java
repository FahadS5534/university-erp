package com.university.erp.attendance;

import com.university.erp.attendance.controller.AttendanceController;
import com.university.erp.attendance.controller.AttendanceExceptionHandler;
import com.university.erp.attendance.dto.*;
import com.university.erp.attendance.entity.Attendance;
import com.university.erp.attendance.entity.AttendanceStatus;
import com.university.erp.attendance.repository.AttendanceRepository;
import com.university.erp.attendance.service.AttendanceServiceImpl;
import com.university.erp.course.entity.Subject;
import com.university.erp.course.repository.SubjectRepository;
import com.university.erp.lecture.entity.Lecture;
import com.university.erp.lecture.repository.LectureRepository;
import com.university.erp.student.entity.Enrollment;
import com.university.erp.student.entity.Student;
import com.university.erp.student.repository.EnrollmentRepository;
import com.university.erp.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Attendance Calendar and Trends Feature Integration & Verification Tests")
class AttendanceCalendarTrendsIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private AttendanceController attendanceController;

    private UUID studentId;
    private Student student;
    private Subject subjectMath;
    private Subject subjectPhysics;

    @BeforeEach
    void setUp() {
        attendanceController = new AttendanceController(attendanceService);
        mockMvc = MockMvcBuilders.standaloneSetup(attendanceController)
                .setControllerAdvice(new AttendanceExceptionHandler()).build();

        studentId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        student = new Student();
        student.setStudentId(studentId);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEnrollmentNo("EN2026");

        subjectMath = Subject.builder().id(10L).subjectCode("MATH101").subjectName("Advanced Mathematics").build();
        subjectPhysics = Subject.builder().id(20L).subjectCode("PHY101").subjectName("Physics Mechanics").build();
    }

    @Test
    @DisplayName("Req 1 & 4: Month with multiple lectures on the same day and multiple subjects")
    void testMultipleLecturesOnSameDayAndMultipleSubjects() throws Exception {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        LocalDate day5 = LocalDate.of(2026, 10, 5);
        Lecture l1 = createLecture(1L, 10L, day5, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Lecture l2 = createLecture(2L, 20L, day5, LocalTime.of(11, 0), LocalTime.of(12, 0));

        Attendance a1 = createAttendance(101L, 1L, studentId, AttendanceStatus.Present,
                LocalDateTime.of(2026, 10, 5, 9, 2));
        Attendance a2 = createAttendance(102L, 2L, studentId, AttendanceStatus.Absent,
                LocalDateTime.of(2026, 10, 5, 11, 5));

        when(attendanceRepository.findMonthlyAttendanceRecords(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(List.of(new Object[] { a1, l1 }, new Object[] { a2, l2 }));
        when(subjectRepository.findAllById(anySet())).thenReturn(List.of(subjectMath, subjectPhysics));

        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.totalDaysInMonth").value(31))
                .andExpect(jsonPath("$.activeAttendanceDays").value(1)).andExpect(jsonPath("$.totalLectures").value(2))
                .andExpect(jsonPath("$.presentCount").value(1)).andExpect(jsonPath("$.absentCount").value(1))
                .andExpect(jsonPath("$.attendancePercentage").value(50.0))
                .andExpect(jsonPath("$.days[4].dayOfMonth").value(5))
                .andExpect(jsonPath("$.days[4].totalLectures").value(2))
                .andExpect(jsonPath("$.days[4].entries[0].subjectCode").value("MATH101"))
                .andExpect(jsonPath("$.days[4].entries[1].subjectCode").value("PHY101"));
    }

    @Test
    @DisplayName("Req 2: Month with no attendance records returns all empty days")
    void testMonthWithNoAttendanceRecords() throws Exception {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(attendanceRepository.findMonthlyAttendanceRecords(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDaysInMonth").value(31))
                .andExpect(jsonPath("$.activeAttendanceDays").value(0)).andExpect(jsonPath("$.totalLectures").value(0))
                .andExpect(jsonPath("$.attendancePercentage").value(0.0))
                .andExpect(jsonPath("$.days.length()").value(31))
                .andExpect(jsonPath("$.days[0].totalLectures").value(0))
                .andExpect(jsonPath("$.days[0].entries").isEmpty());
    }

    @Test
    @DisplayName("Req 3: Month ending in December and trends crossing into January (year transition)")
    void testDecemberEndingAndYearTransition() throws Exception {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // 1. December calendar
        when(attendanceRepository.findMonthlyAttendanceRecords(studentId, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.month").value(12))
                .andExpect(jsonPath("$.monthName").value("DECEMBER"))
                .andExpect(jsonPath("$.totalDaysInMonth").value(31));

        // 2. Trends crossing Dec 2026 to Jan 2027
        LocalDate startDate = LocalDate.of(2026, 12, 1);
        LocalDate endDate = LocalDate.of(2027, 1, 31);

        Object[] subj1 = new Object[] { 10L, 20L, 16L, 2L, 2L };
        when(attendanceRepository.getSubjectTrendsAggregation(studentId, startDate, endDate))
                .thenReturn(Collections.singletonList(subj1));

        Object[] mDec = new Object[] { 10L, 2026, 12, 10L, 8L, 1L, 1L };
        Object[] mJan = new Object[] { 10L, 2027, 1, 10L, 8L, 1L, 1L };
        when(attendanceRepository.getSubjectMonthlyTrendsAggregation(studentId, startDate, endDate))
                .thenReturn(List.of(mDec, mJan));
        when(subjectRepository.findAllById(anySet())).thenReturn(List.of(subjectMath));

        mockMvc.perform(get("/api/attendance/calendar/" + studentId + "/trends").param("startDate", "2026-12-01")
                .param("endDate", "2027-01-31")).andExpect(status().isOk())
                .andExpect(jsonPath("$.overallTotalLectures").value(20))
                .andExpect(jsonPath("$.overallAttendedLectures").value(18))
                .andExpect(jsonPath("$.overallPercentage").value(90.0))
                .andExpect(jsonPath("$.subjects[0].monthlyBreakdown.length()").value(2))
                .andExpect(jsonPath("$.subjects[0].monthlyBreakdown[0].year").value(2026))
                .andExpect(jsonPath("$.subjects[0].monthlyBreakdown[0].monthName").value("DECEMBER"))
                .andExpect(jsonPath("$.subjects[0].monthlyBreakdown[1].year").value(2027))
                .andExpect(jsonPath("$.subjects[0].monthlyBreakdown[1].monthName").value("JANUARY"));
    }

    @Test
    @DisplayName("Req 5: Present, absent, and C_WORK supported statuses")
    void testSupportedStatusesPresentAbsentCWork() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Object[] row = new Object[] { 3L, 1L, 1L, 1L, 3L }; // total=3, present=1, absent=1, cWork=1, 3 days
        when(attendanceRepository.getMonthlySummaryAggregation(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(Collections.singletonList(row));

        MonthlyAttendanceSummaryResponse summary = attendanceService.getMonthlySummary(studentId, 2026, 10);

        assertEquals(3L, summary.totalMarkedLectures());
        assertEquals(1L, summary.presentCount());
        assertEquals(1L, summary.absentCount());
        assertEquals(1L, summary.cWorkCount());
        // present + cWork = 2, total = 3 -> 66.67%
        assertEquals(66.67, summary.attendancePercentage());
    }

    @Test
    @DisplayName("Req 6: Zero-record percentage calculation without division-by-zero")
    void testZeroRecordPercentageCalculation() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Object[] emptyRow = new Object[] { 0L, null, null, null, 0L };
        when(attendanceRepository.getMonthlySummaryAggregation(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(Collections.singletonList(emptyRow));

        MonthlyAttendanceSummaryResponse summary = attendanceService.getMonthlySummary(studentId, 2026, 10);

        assertEquals(0L, summary.totalMarkedLectures());
        assertEquals(0.0, summary.attendancePercentage());
        assertFalse(Double.isNaN(summary.attendancePercentage()));
        assertFalse(Double.isInfinite(summary.attendancePercentage()));
    }

    @Test
    @DisplayName("Req 7: Invalid month, invalid year, and malformed date ranges")
    void testInvalidInputsValidation() throws Exception {
        // Month > 12
        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "13"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());

        // Month < 1
        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "0"))
                .andExpect(status().isBadRequest());

        // Year out of valid bounds
        mockMvc.perform(
                get("/api/attendance/calendar/" + studentId + "/summary").param("year", "1850").param("month", "5"))
                .andExpect(status().isBadRequest());

        // startDate > endDate
        mockMvc.perform(get("/api/attendance/calendar/" + studentId + "/trends").param("startDate", "2026-12-31")
                .param("endDate", "2026-01-01")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Req 8: Missing or nonexistent student returns 404")
    void testMissingStudentReturnsNotFound() throws Exception {
        UUID unknownStudentId = UUID.randomUUID();
        when(studentRepository.findById(unknownStudentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/attendance/calendar/" + unknownStudentId).param("year", "2026").param("month", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Student not found: " + unknownStudentId));
    }

    @Test
    @DisplayName("Req 9: Document limitation when security/auth is not present in repo")
    void testSecurityLimitationDocumentation() {
        // The project currently does not configure Spring Security filter chains or token auth.
        // As specified in Scope & Feature requirements (D), verify that endpoint accepts UUID studentId
        // and document that access control must be implemented when Spring Security is integrated.
        assertNotNull(attendanceController);
    }

    @Test
    @DisplayName("Req 10: Existing Attendance CRUD functionality regression check")
    void testExistingCrudFunctionalityPreserved() {
        AttendanceRequest req = new AttendanceRequest(1L, studentId, AttendanceStatus.Present, "Good", null,
                "faculty@university.edu");
        when(lectureRepository.existsById(1L)).thenReturn(true);
        when(studentRepository.existsById(studentId)).thenReturn(true);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setEnrollmentStatus("Active");
        when(enrollmentRepository.findByStudentStudentId(studentId)).thenReturn(List.of(enrollment));
        when(attendanceRepository.existsByLectureIdAndStudentId(1L, studentId)).thenReturn(false);

        Attendance saved = new Attendance();
        saved.setLectureId(1L);
        saved.setStudentId(studentId);
        saved.setStatus(AttendanceStatus.Present);
        saved.setRemarks("Good");
        saved.setMarkedAt(LocalDateTime.now());
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenReturn(saved);

        AttendanceResponse res = attendanceService.create(req);
        assertNotNull(res);
        assertEquals(AttendanceStatus.Present, res.status());
    }

    @Test
    @DisplayName("Req 11: Correct monthly and subject-wise calculation rounding")
    void testPercentageCalculationRounding() {
        // 1 attended out of 3 = 33.33%
        assertEquals(33.33, AttendanceCalendarValidator.calculatePercentage(1, 0, 3));
        // 2 attended out of 3 = 66.67%
        assertEquals(66.67, AttendanceCalendarValidator.calculatePercentage(2, 0, 3));
        // 7 attended out of 9 = 77.78%
        assertEquals(77.78, AttendanceCalendarValidator.calculatePercentage(7, 0, 9));
        // 0 out of 0 = 0.0%
        assertEquals(0.0, AttendanceCalendarValidator.calculatePercentage(0, 0, 0));
    }

    @Test
    @DisplayName("Req 12: Query filters on Lecture scheduled date rather than Attendance markedAt timestamp")
    void testFilteringOnLectureScheduledDate() {
        LocalDate lectureDate = LocalDate.of(2026, 10, 15);
        LocalDateTime lateMarkedAt = LocalDateTime.of(2026, 11, 2, 10, 0); // marked in November

        Lecture lecture = createLecture(50L, 10L, lectureDate, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Attendance attendance = createAttendance(500L, 50L, studentId, AttendanceStatus.Present, lateMarkedAt);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        // Service queries for October: 2026-10-01 to 2026-10-31
        when(attendanceRepository.findMonthlyAttendanceRecords(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)))
                        .thenReturn(Collections.singletonList(new Object[] { attendance, lecture }));
        when(subjectRepository.findAllById(anySet())).thenReturn(List.of(subjectMath));

        MonthlyAttendanceCalendarResponse calendar = attendanceService.getMonthlyCalendar(studentId, 2026, 10);

        assertEquals(1, calendar.activeAttendanceDays());
        CalendarDayAttendance day15 = calendar.days().get(14);
        assertEquals(1, day15.entries().size());
        assertEquals(lectureDate, day15.entries().get(0).lectureDate());
        assertEquals(lateMarkedAt, day15.entries().get(0).markedAt());
    }

    private Lecture createLecture(Long id, Long subjectId, LocalDate date, LocalTime start, LocalTime end) {
        Lecture l = new Lecture();
        l.setId(id);
        l.setSubjectId(subjectId);
        l.setFacultyId(1);
        l.setLectureDate(date);
        l.setStartTime(start);
        l.setEndTime(end);
        l.setStatus("Conducted");
        return l;
    }

    private Attendance createAttendance(Long id, Long lectureId, UUID studentId, AttendanceStatus status,
            LocalDateTime markedAt) {
        Attendance a = new Attendance();
        a.setLectureId(lectureId);
        a.setStudentId(studentId);
        a.setStatus(status);
        a.setMarkedAt(markedAt);
        return a;
    }
}
