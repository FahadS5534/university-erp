package com.university.erp.attendance.service;

import com.university.erp.attendance.dto.*;
import com.university.erp.attendance.entity.Attendance;
import com.university.erp.attendance.entity.AttendanceStatus;
import com.university.erp.attendance.repository.AttendanceRepository;
import com.university.erp.course.entity.Subject;
import com.university.erp.course.repository.SubjectRepository;
import com.university.erp.lecture.entity.Lecture;
import com.university.erp.lecture.repository.LectureRepository;
import com.university.erp.student.entity.Enrollment;
import com.university.erp.student.entity.Student;
import com.university.erp.student.repository.EnrollmentRepository;
import com.university.erp.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

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

    private UUID sampleStudentId;
    private Long sampleLectureId;
    private Attendance sampleAttendance;
    private Lecture sampleLecture;
    private Student sampleStudent;
    private Enrollment sampleEnrollment;

    @BeforeEach
    void setUp() {
        sampleStudentId = UUID.randomUUID();
        sampleLectureId = 1L;

        sampleAttendance = new Attendance();
        sampleAttendance.setLectureId(sampleLectureId);
        sampleAttendance.setStudentId(sampleStudentId);
        sampleAttendance.setStatus(AttendanceStatus.Present);
        sampleAttendance.setRemarks("On time");
        sampleAttendance.setMarkedAt(LocalDateTime.now());
        sampleAttendance.setCreatedBy("prof@university.edu");

        sampleLecture = new Lecture();
        sampleLecture.setId(sampleLectureId);
        sampleLecture.setSubjectId(10L);
        sampleLecture.setFacultyId(5);
        sampleLecture.setLectureDate(LocalDate.now());
        sampleLecture.setStartTime(LocalTime.of(9, 0));
        sampleLecture.setEndTime(LocalTime.of(10, 0));
        sampleLecture.setLectureType("Theory");
        sampleLecture.setStatus("Conducted");

        sampleStudent = new Student();
        sampleStudent.setStudentId(sampleStudentId);
        sampleStudent.setEnrollmentNo("EN1001");
        sampleStudent.setFirstName("Alice");
        sampleStudent.setLastName("Smith");

        sampleEnrollment = new Enrollment();
        sampleEnrollment.setStudent(sampleStudent);
        sampleEnrollment.setDivision("A");
        sampleEnrollment.setRollNo(101);
        sampleEnrollment.setEnrollmentStatus("Active");
    }

    @Test
    void testCreate_Success() {
        AttendanceRequest request = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Present,
                "Present on time", null, "prof@university.edu");

        when(lectureRepository.existsById(sampleLectureId)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));
        when(attendanceRepository.existsByLectureIdAndStudentId(sampleLectureId, sampleStudentId)).thenReturn(false);
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenReturn(sampleAttendance);

        AttendanceResponse response = attendanceService.create(request);

        assertNotNull(response);
        assertEquals(sampleLectureId, response.lectureId());
        assertEquals(sampleStudentId, response.studentId());
        assertEquals(AttendanceStatus.Present, response.status());
        verify(attendanceRepository).saveAndFlush(any(Attendance.class));
    }

    @Test
    void testCreate_LectureNotFound_Throws404() {
        AttendanceRequest request = new AttendanceRequest(999L, sampleStudentId, AttendanceStatus.Present, null, null,
                null);

        when(lectureRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.create(request));
        assertEquals(404, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Lecture not found"));
    }

    @Test
    void testCreate_StudentNotFound_Throws404() {
        AttendanceRequest request = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Present,
                null, null, null);

        when(lectureRepository.existsById(sampleLectureId)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.create(request));
        assertEquals(404, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Student not found"));
    }

    @Test
    void testCreate_StudentNotActivelyEnrolled_Throws400() {
        Enrollment inactiveEnrollment = new Enrollment();
        inactiveEnrollment.setEnrollmentStatus("Inactive");

        AttendanceRequest request = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Present,
                null, null, null);

        when(lectureRepository.existsById(sampleLectureId)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(inactiveEnrollment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.create(request));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("does not have an active enrollment"));
    }

    @Test
    void testCreate_DuplicateRecord_Throws409() {
        AttendanceRequest request = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Present,
                null, null, null);

        when(lectureRepository.existsById(sampleLectureId)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));
        when(attendanceRepository.existsByLectureIdAndStudentId(sampleLectureId, sampleStudentId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.create(request));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("already marked"));
    }

    @Test
    void testCreate_DataIntegrityViolation_Throws409() {
        AttendanceRequest request = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Present,
                null, null, null);

        when(lectureRepository.existsById(sampleLectureId)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));
        when(attendanceRepository.existsByLectureIdAndStudentId(sampleLectureId, sampleStudentId)).thenReturn(false);
        when(attendanceRepository.saveAndFlush(any(Attendance.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.create(request));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void testCreate_MissingFields_Throws400() {
        AttendanceRequest invalid = new AttendanceRequest(null, sampleStudentId, AttendanceStatus.Present, null, null,
                null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.create(invalid));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testGet_Success() {
        when(attendanceRepository.findById(10L)).thenReturn(Optional.of(sampleAttendance));

        AttendanceResponse response = attendanceService.get(10L);

        assertNotNull(response);
        assertEquals(sampleLectureId, response.lectureId());
    }

    @Test
    void testGet_NotFound() {
        when(attendanceRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> attendanceService.get(99L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testList_UnpagedWithFilters() {
        when(attendanceRepository.findWithFilters(eq(sampleLectureId), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull())).thenReturn(List.of(sampleAttendance));

        List<AttendanceResponse> results = attendanceService.list(sampleLectureId, null, null);

        assertEquals(1, results.size());
        assertEquals(sampleLectureId, results.get(0).lectureId());
    }

    @Test
    void testListPaged_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
        Page<Attendance> page = new PageImpl<>(List.of(sampleAttendance), pageable, 1);

        when(attendanceRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(page);

        Page<AttendanceResponse> pagedResult = attendanceService.listPaged(null, null, null, null, null, null, null,
                pageable);

        assertNotNull(pagedResult);
        assertEquals(1, pagedResult.getTotalElements());
        assertEquals(sampleLectureId, pagedResult.getContent().get(0).lectureId());
    }

    @Test
    void testUpdate_Success() {
        AttendanceRequest updateReq = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Absent,
                "Sick leave", null, "prof@university.edu");

        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(sampleAttendance));
        when(lectureRepository.existsById(sampleLectureId)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));
        when(attendanceRepository.saveAndFlush(any(Attendance.class))).thenReturn(sampleAttendance);

        AttendanceResponse response = attendanceService.update(5L, updateReq);

        assertNotNull(response);
        verify(attendanceRepository).saveAndFlush(sampleAttendance);
    }

    @Test
    void testUpdate_ConflictOnChangingToDuplicate() {
        AttendanceRequest updateReq = new AttendanceRequest(2L, sampleStudentId, AttendanceStatus.Present, null, null,
                null);

        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(sampleAttendance));
        when(lectureRepository.existsById(2L)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));
        when(attendanceRepository.existsByLectureIdAndStudentId(2L, sampleStudentId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.update(5L, updateReq));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void testDelete_Success() {
        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(sampleAttendance));
        when(lectureRepository.findById(sampleLectureId)).thenReturn(Optional.of(sampleLecture));

        attendanceService.delete(5L);

        verify(attendanceRepository).delete(sampleAttendance);
    }

    @Test
    void testDelete_ArchivedLecture_ThrowsBadRequest() {
        sampleLecture.setStatus("Archived");
        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(sampleAttendance));
        when(lectureRepository.findById(sampleLectureId)).thenReturn(Optional.of(sampleLecture));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> attendanceService.delete(5L));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("archived lecture"));
        verify(attendanceRepository, never()).delete(any());
    }

    @Test
    void testMarkBulk_Success() {
        UUID student2 = UUID.randomUUID();
        AttendanceRequest req1 = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Present, null,
                null, "prof");
        AttendanceRequest req2 = new AttendanceRequest(sampleLectureId, student2, AttendanceStatus.Absent, null, null,
                "prof");

        when(lectureRepository.existsById(sampleLectureId)).thenReturn(true);
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(studentRepository.existsById(student2)).thenReturn(true);
        when(enrollmentRepository.findByStudentStudentId(any())).thenReturn(List.of(sampleEnrollment));

        when(attendanceRepository.findByLectureIdAndStudentId(sampleLectureId, sampleStudentId))
                .thenReturn(Optional.empty());
        when(attendanceRepository.findByLectureIdAndStudentId(sampleLectureId, student2)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(sampleAttendance);

        List<AttendanceResponse> responses = attendanceService.markBulk(List.of(req1, req2));

        assertEquals(2, responses.size());
        verify(attendanceRepository, times(2)).save(any(Attendance.class));
        verify(attendanceRepository).flush();
    }

    @Test
    void testMarkBulk_DuplicateInBatch_ThrowsBadRequest() {
        AttendanceRequest req1 = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Present, null,
                null, "prof");
        AttendanceRequest req2 = new AttendanceRequest(sampleLectureId, sampleStudentId, AttendanceStatus.Absent, null,
                null, "prof");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.markBulk(List.of(req1, req2)));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Duplicate student and lecture in batch"));
    }

    @Test
    void testMarkBulk_LectureNotFound_ThrowsNotFound() {
        AttendanceRequest req1 = new AttendanceRequest(999L, sampleStudentId, AttendanceStatus.Present, null, null,
                "prof");
        when(lectureRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.markBulk(List.of(req1)));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testMarkBulk_EmptyList_ThrowsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.markBulk(Collections.emptyList()));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testSummary_Success() {
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(attendanceRepository.countByStudentId(sampleStudentId)).thenReturn(10L);
        when(attendanceRepository.countByStudentIdAndStatus(sampleStudentId, AttendanceStatus.Present)).thenReturn(7L);
        when(attendanceRepository.countByStudentIdAndStatus(sampleStudentId, AttendanceStatus.Absent)).thenReturn(2L);
        when(attendanceRepository.countByStudentIdAndStatus(sampleStudentId, AttendanceStatus.C_WORK)).thenReturn(1L);

        AttendanceSummary summary = attendanceService.summary(sampleStudentId, null);

        assertNotNull(summary);
        assertEquals(10L, summary.total());
        assertEquals(7L, summary.present());
        assertEquals(2L, summary.absent());
        assertEquals(1L, summary.cWork());
        assertEquals(80.0, summary.percentage());
    }

    @Test
    void testSummary_StudentNotFound_ThrowsNotFound() {
        when(studentRepository.existsById(sampleStudentId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.summary(sampleStudentId, null));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testSummary_ZeroLectures_ReturnsZeroPercentage() {
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        when(attendanceRepository.countByStudentId(sampleStudentId)).thenReturn(0L);

        AttendanceSummary summary = attendanceService.summary(sampleStudentId, null);

        assertEquals(0L, summary.total());
        assertEquals(0.0, summary.percentage());
    }

    @Test
    void testGetSubjectSummaries_Success() {
        when(studentRepository.existsById(sampleStudentId)).thenReturn(true);
        Object[] row1 = new Object[] { 10L, 20L, 16L, 2L, 2L };
        when(attendanceRepository.getSubjectAttendanceSummaryByStudentId(sampleStudentId))
                .thenReturn(Collections.singletonList(row1));

        Subject subject = Subject.builder().id(10L).subjectCode("CS101").subjectName("Data Structures").build();
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));

        List<SubjectAttendanceSummary> summaries = attendanceService.getSubjectSummaries(sampleStudentId);

        assertEquals(1, summaries.size());
        assertEquals("CS101", summaries.get(0).subjectCode());
        assertEquals("Data Structures", summaries.get(0).subjectName());
        assertEquals(20L, summaries.get(0).totalLectures());
        assertEquals(90.0, summaries.get(0).percentage());
    }

    @Test
    void testGetLectureRegister_Success() {
        when(lectureRepository.findById(sampleLectureId)).thenReturn(Optional.of(sampleLecture));
        Subject subject = Subject.builder().id(10L).subjectName("Data Structures").build();
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(attendanceRepository.findByLectureId(sampleLectureId)).thenReturn(List.of(sampleAttendance));

        LectureAttendanceRegister register = attendanceService.getLectureRegister(sampleLectureId);

        assertNotNull(register);
        assertEquals(sampleLectureId, register.lectureId());
        assertEquals("Data Structures", register.subjectName());
        assertEquals(1, register.totalMarked());
        assertEquals(1, register.presentCount());
        assertEquals(100.0, register.attendanceRate());
    }

    @Test
    void testGetStudentHistory_Success() {
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));
        when(attendanceRepository.findWithFilters(any(), eq(sampleStudentId), any(), any(), any(), any(), any()))
                .thenReturn(List.of(sampleAttendance));
        when(lectureRepository.findById(sampleLectureId)).thenReturn(Optional.of(sampleLecture));

        StudentAttendanceHistoryResponse history = attendanceService.getStudentHistory(sampleStudentId, null, null,
                null);

        assertNotNull(history);
        assertEquals(sampleStudentId, history.studentId());
        assertEquals("Alice Smith", history.studentName());
        assertEquals(1, history.totalLectures());
        assertEquals(1, history.attendedLectures());
        assertEquals(100.0, history.overallPercentage());
        assertEquals(1, history.records().size());
    }

    @Test
    void testGetDefaulters_Success() {
        Object[] row1 = new Object[] { sampleStudentId, 40L, 20L }; // 50% attendance
        when(attendanceRepository.getStudentOverallAttendanceCounts(null)).thenReturn(Collections.singletonList(row1));
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));

        List<AttendanceDefaulterResponse> defaulters = attendanceService.getDefaulters(75.0, null, null);

        assertEquals(1, defaulters.size());
        assertEquals(sampleStudentId, defaulters.get(0).studentId());
        assertEquals(50.0, defaulters.get(0).percentage());
        assertEquals("Warning", defaulters.get(0).status());
    }

    @Test
    void testGetDefaulters_FiltersByDivision() {
        Object[] row1 = new Object[] { sampleStudentId, 40L, 20L };
        when(attendanceRepository.getStudentOverallAttendanceCounts(null)).thenReturn(Collections.singletonList(row1));
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));
        when(enrollmentRepository.findByStudentStudentId(sampleStudentId)).thenReturn(List.of(sampleEnrollment));

        List<AttendanceDefaulterResponse> defaulters = attendanceService.getDefaulters(75.0, null, "B");

        assertTrue(defaulters.isEmpty());
    }

    @Test
    void testGetMonthlyCalendar_Success_MultipleLecturesPerDay() {
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));

        LocalDate date15 = LocalDate.of(2026, 10, 15);
        LocalDate date16 = LocalDate.of(2026, 10, 16);

        Lecture l1 = new Lecture();
        l1.setId(101L);
        l1.setSubjectId(501L);
        l1.setLectureDate(date15);
        l1.setStartTime(LocalTime.of(9, 0));

        Attendance a1 = new Attendance();
        a1.setLectureId(101L);
        a1.setStudentId(sampleStudentId);
        a1.setStatus(AttendanceStatus.Present);
        a1.setMarkedAt(LocalDateTime.of(2026, 10, 15, 9, 5));

        Lecture l2 = new Lecture();
        l2.setId(102L);
        l2.setSubjectId(502L);
        l2.setLectureDate(date15);
        l2.setStartTime(LocalTime.of(11, 0));

        Attendance a2 = new Attendance();
        a2.setLectureId(102L);
        a2.setStudentId(sampleStudentId);
        a2.setStatus(AttendanceStatus.Absent);
        a2.setMarkedAt(LocalDateTime.of(2026, 10, 15, 11, 5));

        Lecture l3 = new Lecture();
        l3.setId(103L);
        l3.setSubjectId(501L);
        l3.setLectureDate(date16);
        l3.setStartTime(LocalTime.of(10, 0));

        Attendance a3 = new Attendance();
        a3.setLectureId(103L);
        a3.setStudentId(sampleStudentId);
        a3.setStatus(AttendanceStatus.C_WORK);
        a3.setMarkedAt(LocalDateTime.of(2026, 10, 16, 10, 5));

        List<Object[]> records = List.of(
                new Object[]{a1, l1},
                new Object[]{a2, l2},
                new Object[]{a3, l3}
        );

        when(attendanceRepository.findMonthlyAttendanceRecords(sampleStudentId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(records);

        Subject s1 = Subject.builder().id(501L).subjectCode("CS101").subjectName("Data Structures").build();
        Subject s2 = Subject.builder().id(502L).subjectCode("CS102").subjectName("Algorithms").build();
        when(subjectRepository.findAllById(anySet())).thenReturn(List.of(s1, s2));

        MonthlyAttendanceCalendarResponse calendar = attendanceService.getMonthlyCalendar(sampleStudentId, 2026, 10);

        assertNotNull(calendar);
        assertEquals(sampleStudentId, calendar.studentId());
        assertEquals("Alice Smith", calendar.studentName());
        assertEquals("EN1001", calendar.enrollmentNo());
        assertEquals(2026, calendar.year());
        assertEquals(10, calendar.month());
        assertEquals("OCTOBER", calendar.monthName());
        assertEquals(31, calendar.totalDaysInMonth());
        assertEquals(2, calendar.activeAttendanceDays());
        assertEquals(31, calendar.days().size());

        CalendarDayAttendance day15 = calendar.days().get(14);
        assertEquals(date15, day15.date());
        assertEquals(2, day15.entries().size());
        assertEquals("Data Structures", day15.entries().get(0).subjectName());
        assertEquals(AttendanceStatus.Present, day15.entries().get(0).status());
        assertEquals("Algorithms", day15.entries().get(1).subjectName());
        assertEquals(AttendanceStatus.Absent, day15.entries().get(1).status());

        CalendarDayAttendance day16 = calendar.days().get(15);
        assertEquals(date16, day16.date());
        assertEquals(1, day16.entries().size());
        assertEquals(AttendanceStatus.C_WORK, day16.entries().get(0).status());

        // Empty day check (e.g., Oct 1st)
        CalendarDayAttendance day1 = calendar.days().get(0);
        assertTrue(day1.entries().isEmpty());

        // Summary check: 3 marked, 1 present, 1 absent, 1 c_work -> (1 + 1) / 3 * 100 = 66.67%
        assertEquals(3L, calendar.totalLectures());
        assertEquals(1L, calendar.presentCount());
        assertEquals(1L, calendar.absentCount());
        assertEquals(1L, calendar.cWorkCount());
        assertEquals(66.67, calendar.attendancePercentage());
    }

    @Test
    void testGetMonthlyCalendar_EmptyMonth() {
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));
        when(attendanceRepository.findMonthlyAttendanceRecords(sampleStudentId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Collections.emptyList());

        MonthlyAttendanceCalendarResponse calendar = attendanceService.getMonthlyCalendar(sampleStudentId, 2026, 10);

        assertNotNull(calendar);
        assertEquals(31, calendar.days().size());
        assertEquals(0, calendar.activeAttendanceDays());
        assertEquals(0L, calendar.totalLectures());
        assertEquals(0.0, calendar.attendancePercentage());
    }

    @Test
    void testGetMonthlyCalendar_StudentNotFound() {
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.getMonthlyCalendar(sampleStudentId, 2026, 10));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Student not found"));
    }

    @Test
    void testGetMonthlyCalendar_InvalidMonth() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.getMonthlyCalendar(sampleStudentId, 2026, 13));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testGetMonthlyCalendar_InvalidYear() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.getMonthlyCalendar(sampleStudentId, 1899, 10));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testGetMonthlySummary_Success() {
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));

        Object[] aggRow = new Object[]{10L, 8L, 1L, 1L, 6L};
        when(attendanceRepository.getMonthlySummaryAggregation(sampleStudentId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Collections.singletonList(aggRow));

        MonthlyAttendanceSummaryResponse summary = attendanceService.getMonthlySummary(sampleStudentId, 2026, 10);

        assertNotNull(summary);
        assertEquals(10L, summary.totalMarkedLectures());
        assertEquals(8L, summary.presentCount());
        assertEquals(1L, summary.absentCount());
        assertEquals(1L, summary.cWorkCount());
        assertEquals(90.0, summary.attendancePercentage());
        assertEquals(6, summary.daysWithAttendance());
    }

    @Test
    void testGetMonthlySummary_ZeroRecords() {
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));

        Object[] emptyRow = new Object[]{0L, null, null, null, 0L};
        when(attendanceRepository.getMonthlySummaryAggregation(sampleStudentId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Collections.singletonList(emptyRow));

        MonthlyAttendanceSummaryResponse summary = attendanceService.getMonthlySummary(sampleStudentId, 2026, 10);

        assertNotNull(summary);
        assertEquals(0L, summary.totalMarkedLectures());
        assertEquals(0L, summary.presentCount());
        assertEquals(0L, summary.absentCount());
        assertEquals(0L, summary.cWorkCount());
        assertEquals(0.0, summary.attendancePercentage());
        assertEquals(0, summary.daysWithAttendance());
    }

    @Test
    void testGetAttendanceTrends_Success_SpanningMonths() {
        when(studentRepository.findById(sampleStudentId)).thenReturn(Optional.of(sampleStudent));

        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);

        Object[] subj1 = new Object[]{101L, 20L, 18L, 2L, 0L};
        Object[] subj2 = new Object[]{102L, 10L, 7L, 2L, 1L};
        when(attendanceRepository.getSubjectTrendsAggregation(sampleStudentId, startDate, endDate))
                .thenReturn(List.of(subj1, subj2));

        Object[] m1 = new Object[]{101L, 2026, 8, 10L, 9L, 1L, 0L};
        Object[] m2 = new Object[]{101L, 2026, 9, 10L, 9L, 1L, 0L};
        when(attendanceRepository.getSubjectMonthlyTrendsAggregation(sampleStudentId, startDate, endDate))
                .thenReturn(List.of(m1, m2));

        Subject s1 = Subject.builder().id(101L).subjectCode("MATH101").subjectName("Calculus").build();
        Subject s2 = Subject.builder().id(102L).subjectCode("PHY101").subjectName("Physics").build();
        when(subjectRepository.findAllById(anySet())).thenReturn(List.of(s1, s2));

        AttendanceTrendsResponse trends = attendanceService.getAttendanceTrends(sampleStudentId, startDate, endDate);

        assertNotNull(trends);
        assertEquals(sampleStudentId, trends.studentId());
        assertEquals("Alice Smith", trends.studentName());
        assertEquals("EN1001", trends.enrollmentNo());
        assertEquals(startDate, trends.startDate());
        assertEquals(endDate, trends.endDate());
        assertEquals(30L, trends.overallTotalLectures());
        assertEquals(26L, trends.overallAttendedLectures());
        assertEquals(86.67, trends.overallPercentage());

        assertEquals(2, trends.subjects().size());
        SubjectAttendanceTrend trend1 = trends.subjects().get(0);
        assertEquals("Calculus", trend1.subjectName());
        assertEquals(90.0, trend1.attendancePercentage());
        assertEquals(2, trend1.monthlyBreakdown().size());
        assertEquals(90.0, trend1.monthlyBreakdown().get(0).percentage());
    }

    @Test
    void testGetAttendanceTrends_InvalidDateRange() {
        LocalDate startDate = LocalDate.of(2026, 10, 31);
        LocalDate endDate = LocalDate.of(2026, 8, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.getAttendanceTrends(sampleStudentId, startDate, endDate));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cannot be after"));
    }
}
