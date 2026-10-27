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
}
