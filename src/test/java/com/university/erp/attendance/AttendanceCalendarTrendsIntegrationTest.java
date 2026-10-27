package com.university.erp.attendance;

import com.university.erp.attendance.controller.AttendanceController;
import com.university.erp.attendance.controller.AttendanceExceptionHandler;
import com.university.erp.attendance.dto.AttendanceCalendarValidator;
import com.university.erp.attendance.dto.AttendanceRequest;
import com.university.erp.attendance.dto.AttendanceResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
@DisplayName("Attendance Calendar & Trends Basic Tests")
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
    private Subject mathSubject;
    private Subject physicsSubject;

    @BeforeEach
    void setUp() {
        attendanceController = new AttendanceController(attendanceService);
        mockMvc = MockMvcBuilders.standaloneSetup(attendanceController)
                .setControllerAdvice(new AttendanceExceptionHandler()).build();

        studentId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        student = new Student();
        student.setStudentId(studentId);
        student.setFirstName("Alice");
        student.setLastName("Smith");
        student.setEnrollmentNo("EN1001");

        mathSubject = Subject.builder().id(10L).subjectCode("MATH101").subjectName("Mathematics").build();
        physicsSubject = Subject.builder().id(20L).subjectCode("PHY101").subjectName("Physics").build();
    }

    @Test
    @DisplayName("1. A valid month returns the expected calendar")
    void testValidMonthReturnsExpectedCalendar() throws Exception {
        // Arrange
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        Lecture lecture = createLecture(1L, 10L, LocalDate.of(2026, 10, 5), LocalTime.of(9, 0), LocalTime.of(10, 0));
        Attendance attendance = createAttendance(101L, 1L, studentId, AttendanceStatus.Present,
                LocalDateTime.of(2026, 10, 5, 9, 5));

        when(attendanceRepository.findMonthlyAttendanceRecords(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)))
                        .thenReturn(Collections.singletonList(new Object[] { attendance, lecture }));
        when(subjectRepository.findAllById(anySet())).thenReturn(Collections.singletonList(mathSubject));

        // Act & Assert
        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.year").value(2026)).andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.totalDaysInMonth").value(31))
                .andExpect(jsonPath("$.activeAttendanceDays").value(1)).andExpect(jsonPath("$.totalLectures").value(1))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.attendancePercentage").value(100.0));
    }

    @Test
    @DisplayName("2. An empty month returns all calendar days with no attendance entries")
    void testEmptyMonthReturnsAllCalendarDaysWithNoAttendanceEntries() throws Exception {
        // Arrange
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(attendanceRepository.findMonthlyAttendanceRecords(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDaysInMonth").value(31))
                .andExpect(jsonPath("$.activeAttendanceDays").value(0)).andExpect(jsonPath("$.totalLectures").value(0))
                .andExpect(jsonPath("$.attendancePercentage").value(0.0))
                .andExpect(jsonPath("$.days.length()").value(31))
                .andExpect(jsonPath("$.days[0].totalLectures").value(0))
                .andExpect(jsonPath("$.days[0].entries").isEmpty());
    }

    @Test
    @DisplayName("3. Multiple lectures on the same date are grouped correctly")
    void testMultipleLecturesOnSameDateGroupedCorrectly() throws Exception {
        // Arrange
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        LocalDate date = LocalDate.of(2026, 10, 5);
        Lecture lecture1 = createLecture(1L, 10L, date, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Lecture lecture2 = createLecture(2L, 20L, date, LocalTime.of(11, 0), LocalTime.of(12, 0));

        Attendance attendance1 = createAttendance(101L, 1L, studentId, AttendanceStatus.Present,
                LocalDateTime.of(2026, 10, 5, 9, 2));
        Attendance attendance2 = createAttendance(102L, 2L, studentId, AttendanceStatus.Absent,
                LocalDateTime.of(2026, 10, 5, 11, 5));

        when(attendanceRepository.findMonthlyAttendanceRecords(studentId, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31))).thenReturn(
                        List.of(new Object[] { attendance1, lecture1 }, new Object[] { attendance2, lecture2 }));
        when(subjectRepository.findAllById(anySet())).thenReturn(List.of(mathSubject, physicsSubject));

        // Act & Assert
        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.days[4].dayOfMonth").value(5))
                .andExpect(jsonPath("$.days[4].totalLectures").value(2))
                .andExpect(jsonPath("$.days[4].presentCount").value(1))
                .andExpect(jsonPath("$.days[4].absentCount").value(1))
                .andExpect(jsonPath("$.days[4].entries[0].subjectCode").value("MATH101"))
                .andExpect(jsonPath("$.days[4].entries[1].subjectCode").value("PHY101"));
    }

    @Test
    @DisplayName("4. Attendance percentage is calculated correctly")
    void testAttendancePercentageCalculatedCorrectly() {
        // Present + C_WORK = attended
        assertEquals(100.0, AttendanceCalendarValidator.calculatePercentage(10, 0, 10));
        assertEquals(75.0, AttendanceCalendarValidator.calculatePercentage(3, 0, 4));
        assertEquals(66.67, AttendanceCalendarValidator.calculatePercentage(1, 1, 3));
        assertEquals(0.0, AttendanceCalendarValidator.calculatePercentage(0, 0, 5));
        // Safe handling when total lectures is 0
        assertEquals(0.0, AttendanceCalendarValidator.calculatePercentage(0, 0, 0));
    }

    @Test
    @DisplayName("5. An invalid month is rejected")
    void testInvalidMonthIsRejected() throws Exception {
        // Month > 12
        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "13"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());

        // Month < 1
        mockMvc.perform(get("/api/attendance/calendar/" + studentId).param("year", "2026").param("month", "0"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("6. A date range with the start date after the end date is rejected")
    void testDateRangeWithStartDateAfterEndDateIsRejected() throws Exception {
        mockMvc.perform(get("/api/attendance/calendar/" + studentId + "/trends").param("startDate", "2026-10-31")
                .param("endDate", "2026-10-01")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("7. A missing student returns the expected error")
    void testMissingStudentReturnsExpectedError() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(studentRepository.findById(unknownId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/attendance/calendar/" + unknownId).param("year", "2026").param("month", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Student not found: " + unknownId));
    }

    @Test
    @DisplayName("8. Existing Attendance CRUD behavior still works")
    void testExistingAttendanceCrudBehaviorWorks() {
        // Arrange
        AttendanceRequest request = new AttendanceRequest(1L, studentId, AttendanceStatus.Present, "Regular", null,
                "faculty@university.edu");
        when(lectureRepository.existsById(1L)).thenReturn(true);
        when(studentRepository.existsById(studentId)).thenReturn(true);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setEnrollmentStatus("Active");
        when(enrollmentRepository.findByStudentStudentId(studentId)).thenReturn(Collections.singletonList(enrollment));
        when(attendanceRepository.existsByLectureIdAndStudentId(1L, studentId)).thenReturn(false);

        Attendance saved = new Attendance();
        saved.setLectureId(1L);
        saved.setStudentId(studentId);
        saved.setStatus(AttendanceStatus.Present);
        saved.setRemarks("Regular");
        saved.setMarkedAt(LocalDateTime.now());
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenReturn(saved);

        // Act
        AttendanceResponse response = attendanceService.create(request);

        // Assert
        assertNotNull(response);
        assertEquals(AttendanceStatus.Present, response.status());
        assertEquals("Regular", response.remarks());
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
