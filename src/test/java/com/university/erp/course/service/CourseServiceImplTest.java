package com.university.erp.course.service;

import com.university.erp.course.dto.CourseRequest;
import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.entity.Course;
import com.university.erp.course.entity.Subject;
import com.university.erp.course.repository.CourseRepository;
import com.university.erp.course.repository.SubjectRepository;
import com.university.erp.department.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        sampleCourse = Course.builder().id(1L).courseCode("CS101").courseName("Computer Science")
                .courseType("Undergraduate").courseDuration(4).departmentId(10).build();
    }

    @Test
    void testCreate_Success() {
        CourseRequest request = new CourseRequest("CS101", "Computer Science", "Undergraduate", 4, 10);
        when(courseRepository.existsByCourseCode("CS101")).thenReturn(false);
        when(departmentRepository.existsById(10)).thenReturn(true);
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        CourseResponse response = courseService.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("CS101", response.courseCode());
        assertEquals("Computer Science", response.courseName());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void testCreate_DuplicateCode_ThrowsConflict() {
        CourseRequest request = new CourseRequest("CS101", "Computer Science", "Undergraduate", 4, 10);
        when(courseRepository.existsByCourseCode("CS101")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> courseService.create(request));
        assertEquals(409, ex.getStatusCode().value());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testCreate_InvalidDepartment_ThrowsNotFound() {
        CourseRequest request = new CourseRequest("CS101", "Computer Science", "Undergraduate", 4, 999);
        when(courseRepository.existsByCourseCode("CS101")).thenReturn(false);
        when(departmentRepository.existsById(999)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> courseService.create(request));
        assertEquals(404, ex.getStatusCode().value());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testCreate_MissingFields_ThrowsBadRequest() {
        CourseRequest invalid = new CourseRequest("", "Name", "Type", 4, 1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> courseService.create(invalid));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testGet_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));

        CourseResponse response = courseService.get(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("CS101", response.courseCode());
    }

    @Test
    void testGet_NotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> courseService.get(99L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testGetByCourseCode_Success() {
        when(courseRepository.findByCourseCode("CS101")).thenReturn(Optional.of(sampleCourse));

        CourseResponse response = courseService.getByCourseCode("CS101");

        assertNotNull(response);
        assertEquals("CS101", response.courseCode());
    }

    @Test
    void testList_WithFilters() {
        when(courseRepository.findByDepartmentIdAndCourseType(10, "Undergraduate")).thenReturn(List.of(sampleCourse));

        List<CourseResponse> list = courseService.list(10, "Undergraduate");

        assertEquals(1, list.size());
        assertEquals("CS101", list.get(0).courseCode());
    }

    @Test
    void testUpdate_Success() {
        CourseRequest updateRequest = new CourseRequest("CS101", "CS Honors", "Undergraduate", 4, 10);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(courseRepository.existsByCourseCodeAndIdNot("CS101", 1L)).thenReturn(false);
        when(departmentRepository.existsById(10)).thenReturn(true);
        when(courseRepository.save(any(Course.class))).thenReturn(sampleCourse);

        CourseResponse response = courseService.update(1L, updateRequest);

        assertNotNull(response);
        verify(courseRepository).save(sampleCourse);
    }

    @Test
    void testDelete_Success() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(subjectRepository.existsByCourseId(1L)).thenReturn(false);

        courseService.delete(1L);

        verify(courseRepository).delete(sampleCourse);
    }

    @Test
    void testDelete_HasSubjects_ThrowsConflict() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(subjectRepository.existsByCourseId(1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> courseService.delete(1L));
        assertEquals(409, ex.getStatusCode().value());
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void testGetSubjectsByCourseId() {
        Subject subject = Subject.builder().id(100L).subjectCode("SUB1").subjectName("Algorithms").courseId(1L)
                .credits((short) 4).subjectType("Core").subjectHours(60).build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));
        when(subjectRepository.findByCourseId(1L)).thenReturn(List.of(subject));

        List<SubjectResponse> subjects = courseService.getSubjectsByCourseId(1L);

        assertEquals(1, subjects.size());
        assertEquals("SUB1", subjects.get(0).subjectCode());
    }
}
