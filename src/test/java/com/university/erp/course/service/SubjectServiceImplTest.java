package com.university.erp.course.service;

import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectRequest;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.entity.Course;
import com.university.erp.course.entity.Subject;
import com.university.erp.course.repository.CourseRepository;
import com.university.erp.course.repository.SubjectRepository;
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
class SubjectServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private Course sampleCourse;
    private Subject sampleSubject;

    @BeforeEach
    void setUp() {
        sampleCourse = Course.builder().id(1L).courseCode("CS101").courseName("Computer Science")
                .courseType("Undergraduate").courseDuration(4).departmentId(10).build();

        sampleSubject = Subject.builder().id(10L).subjectCode("CS101-DS").subjectName("Data Structures").courseId(1L)
                .inSemester((short) 2).credits((short) 4).subjectType("Core").subjectHours(60).build();
    }

    @Test
    void testCreate_Success() {
        SubjectRequest request = new SubjectRequest("CS101-DS", "Data Structures", 1L, (short) 2, (short) 4, "Core",
                60);
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.existsBySubjectCode("CS101-DS")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(sampleSubject);

        SubjectResponse response = subjectService.create(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("CS101-DS", response.subjectCode());
        assertEquals(1L, response.courseId());
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    void testCreate_CourseNotFound_ThrowsNotFound() {
        SubjectRequest request = new SubjectRequest("CS101-DS", "Data Structures", 999L, (short) 2, (short) 4, "Core",
                60);
        when(courseRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subjectService.create(request));
        assertEquals(404, ex.getStatusCode().value());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void testCreate_DuplicateCode_ThrowsConflict() {
        SubjectRequest request = new SubjectRequest("CS101-DS", "Data Structures", 1L, (short) 2, (short) 4, "Core",
                60);
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.existsBySubjectCode("CS101-DS")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subjectService.create(request));
        assertEquals(409, ex.getStatusCode().value());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void testCreate_InvalidInput_ThrowsBadRequest() {
        SubjectRequest invalid = new SubjectRequest("", "Name", 1L, (short) 1, (short) -1, "Core", 50);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subjectService.create(invalid));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void testGet_Success() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));

        SubjectResponse response = subjectService.get(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("CS101-DS", response.subjectCode());
    }

    @Test
    void testGet_NotFound() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> subjectService.get(999L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void testGetBySubjectCode_Success() {
        when(subjectRepository.findBySubjectCode("CS101-DS")).thenReturn(Optional.of(sampleSubject));

        SubjectResponse response = subjectService.getBySubjectCode("CS101-DS");

        assertNotNull(response);
        assertEquals("CS101-DS", response.subjectCode());
    }

    @Test
    void testList_WithFilters() {
        when(subjectRepository.findByCourseIdAndInSemester(1L, (short) 2)).thenReturn(List.of(sampleSubject));

        List<SubjectResponse> result = subjectService.list(1L, (short) 2, "Core");

        assertEquals(1, result.size());
        assertEquals("CS101-DS", result.get(0).subjectCode());
    }

    @Test
    void testGetCourseBySubjectId_Success() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));

        CourseResponse courseResponse = subjectService.getCourseBySubjectId(10L);

        assertNotNull(courseResponse);
        assertEquals(1L, courseResponse.id());
        assertEquals("CS101", courseResponse.courseCode());
    }

    @Test
    void testUpdate_Success() {
        SubjectRequest updateRequest = new SubjectRequest("CS101-DS", "Advanced Data Structures", 1L, (short) 2,
                (short) 4, "Core", 60);
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(subjectRepository.existsBySubjectCodeAndIdNot("CS101-DS", 10L)).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(sampleSubject);

        SubjectResponse response = subjectService.update(10L, updateRequest);

        assertNotNull(response);
        verify(subjectRepository).save(sampleSubject);
    }

    @Test
    void testDelete_Success() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(sampleSubject));

        subjectService.delete(10L);

        verify(subjectRepository).delete(sampleSubject);
    }
}
