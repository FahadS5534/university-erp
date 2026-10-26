package com.university.erp.course.controller;

import com.university.erp.course.dto.CourseRequest;
import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    private CourseResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController).build();
        sampleResponse = new CourseResponse(1L, "CS101", "Computer Science", "Undergraduate", 4, 10);
    }

    @Test
    void testCreateCourse() throws Exception {
        when(courseService.create(any(CourseRequest.class))).thenReturn(sampleResponse);

        String json = """
                {
                    "courseCode": "CS101",
                    "courseName": "Computer Science",
                    "courseType": "Undergraduate",
                    "courseDuration": 4,
                    "departmentId": 10
                }
                """;

        mockMvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.courseCode").value("CS101"))
                .andExpect(jsonPath("$.courseName").value("Computer Science"));
    }

    @Test
    void testGetCourse() throws Exception {
        when(courseService.get(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/courses/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.courseCode").value("CS101"));
    }

    @Test
    void testGetByCourseCode() throws Exception {
        when(courseService.getByCourseCode("CS101")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/courses/code/CS101")).andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("CS101"));
    }

    @Test
    void testListCourses() throws Exception {
        when(courseService.list(10, "Undergraduate")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/courses").param("departmentId", "10").param("courseType", "Undergraduate"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].courseCode").value("CS101"));
    }

    @Test
    void testUpdateCourse() throws Exception {
        when(courseService.update(eq(1L), any(CourseRequest.class))).thenReturn(sampleResponse);

        String json = """
                {
                    "courseCode": "CS101",
                    "courseName": "Computer Science",
                    "courseType": "Undergraduate",
                    "courseDuration": 4,
                    "departmentId": 10
                }
                """;

        mockMvc.perform(put("/api/courses/1").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void testDeleteCourse() throws Exception {
        doNothing().when(courseService).delete(1L);

        mockMvc.perform(delete("/api/courses/1")).andExpect(status().isNoContent());

        verify(courseService).delete(1L);
    }

    @Test
    void testGetSubjectsByCourse() throws Exception {
        SubjectResponse subject = new SubjectResponse(10L, "CS-DS", "Data Structures", 1L, (short) 2, (short) 4, "Core",
                60);
        when(courseService.getSubjectsByCourseId(1L)).thenReturn(List.of(subject));

        mockMvc.perform(get("/api/courses/1/subjects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L)).andExpect(jsonPath("$[0].subjectCode").value("CS-DS"));
    }
}
