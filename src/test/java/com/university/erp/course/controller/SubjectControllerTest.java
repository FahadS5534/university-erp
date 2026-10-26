package com.university.erp.course.controller;

import com.university.erp.course.dto.CourseResponse;
import com.university.erp.course.dto.SubjectRequest;
import com.university.erp.course.dto.SubjectResponse;
import com.university.erp.course.service.SubjectService;
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
class SubjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubjectService subjectService;

    @InjectMocks
    private SubjectController subjectController;

    private SubjectResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subjectController).build();
        sampleResponse = new SubjectResponse(10L, "CS-DS", "Data Structures", 1L, (short) 2, (short) 4, "Core", 60);
    }

    @Test
    void testCreateSubject() throws Exception {
        when(subjectService.create(any(SubjectRequest.class))).thenReturn(sampleResponse);

        String json = """
                {
                    "subjectCode": "CS-DS",
                    "subjectName": "Data Structures",
                    "courseId": 1,
                    "inSemester": 2,
                    "credits": 4,
                    "subjectType": "Core",
                    "subjectHours": 60
                }
                """;

        mockMvc.perform(post("/api/subjects").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.subjectCode").value("CS-DS"))
                .andExpect(jsonPath("$.subjectName").value("Data Structures"));
    }

    @Test
    void testGetSubject() throws Exception {
        when(subjectService.get(10L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/subjects/10")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.subjectCode").value("CS-DS"));
    }

    @Test
    void testGetBySubjectCode() throws Exception {
        when(subjectService.getBySubjectCode("CS-DS")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/subjects/code/CS-DS")).andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectCode").value("CS-DS"));
    }

    @Test
    void testListSubjects() throws Exception {
        when(subjectService.list(1L, (short) 2, "Core")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/subjects").param("courseId", "1").param("semester", "2").param("subjectType", "Core"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].subjectCode").value("CS-DS"));
    }

    @Test
    void testGetCourseBySubject() throws Exception {
        CourseResponse courseResponse = new CourseResponse(1L, "CS101", "Computer Science", "Undergraduate", 4, 10);
        when(subjectService.getCourseBySubjectId(10L)).thenReturn(courseResponse);

        mockMvc.perform(get("/api/subjects/10/course")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.courseCode").value("CS101"));
    }

    @Test
    void testUpdateSubject() throws Exception {
        when(subjectService.update(eq(10L), any(SubjectRequest.class))).thenReturn(sampleResponse);

        String json = """
                {
                    "subjectCode": "CS-DS",
                    "subjectName": "Data Structures",
                    "courseId": 1,
                    "inSemester": 2,
                    "credits": 4,
                    "subjectType": "Core",
                    "subjectHours": 60
                }
                """;

        mockMvc.perform(put("/api/subjects/10").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void testDeleteSubject() throws Exception {
        doNothing().when(subjectService).delete(10L);

        mockMvc.perform(delete("/api/subjects/10")).andExpect(status().isNoContent());

        verify(subjectService).delete(10L);
    }
}
