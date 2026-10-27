package com.university.erp.attendance.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AttendanceRepository Date-Based Query Tests")
class AttendanceRepositoryQueryTest {

    @Test
    @DisplayName("findMonthlyAttendanceRecords has proper signature and JPQL query")
    void testFindMonthlyAttendanceRecordsQuery() throws Exception {
        Method method = AttendanceRepository.class.getMethod("findMonthlyAttendanceRecords",
                UUID.class, LocalDate.class, LocalDate.class);
        assertNotNull(method);

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "findMonthlyAttendanceRecords must have @Query annotation");
        String queryString = query.value();

        assertTrue(queryString.contains("Attendance a, Lecture l"), "Must join Attendance and Lecture");
        assertTrue(queryString.contains("a.lectureId = l.id"), "Must join on lectureId");
        assertTrue(queryString.contains("a.studentId = :studentId"), "Must filter by studentId");
        assertTrue(queryString.contains("l.lectureDate >= :startDate"), "Must filter on lectureDate start");
        assertTrue(queryString.contains("l.lectureDate <= :endDate"), "Must filter on lectureDate end");
        assertTrue(queryString.contains("cancelled"), "Must exclude cancelled lectures");
        assertTrue(queryString.contains("ORDER BY l.lectureDate ASC"), "Must order by lectureDate ASC");

        Parameter[] params = method.getParameters();
        assertEquals("studentId", params[0].getAnnotation(Param.class).value());
        assertEquals("startDate", params[1].getAnnotation(Param.class).value());
        assertEquals("endDate", params[2].getAnnotation(Param.class).value());
    }

    @Test
    @DisplayName("findByStudentIdAndLectureDateBetween has proper signature and JPQL query")
    void testFindByStudentIdAndLectureDateBetweenQuery() throws Exception {
        Method method = AttendanceRepository.class.getMethod("findByStudentIdAndLectureDateBetween",
                UUID.class, LocalDate.class, LocalDate.class);
        assertNotNull(method);

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "findByStudentIdAndLectureDateBetween must have @Query annotation");
        String queryString = query.value();

        assertTrue(queryString.contains("SELECT a FROM Attendance a, Lecture l"));
        assertTrue(queryString.contains("l.lectureDate >= :startDate"));
        assertTrue(queryString.contains("l.lectureDate <= :endDate"));
    }

    @Test
    @DisplayName("getMonthlySummaryAggregation has proper signature and aggregation clauses")
    void testGetMonthlySummaryAggregationQuery() throws Exception {
        Method method = AttendanceRepository.class.getMethod("getMonthlySummaryAggregation",
                UUID.class, LocalDate.class, LocalDate.class);
        assertNotNull(method);

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "getMonthlySummaryAggregation must have @Query annotation");
        String queryString = query.value();

        assertTrue(queryString.contains("COUNT(a)"), "Must count total marked attendance");
        assertTrue(queryString.contains("AttendanceStatus.Present"), "Must aggregate Present status");
        assertTrue(queryString.contains("AttendanceStatus.Absent"), "Must aggregate Absent status");
        assertTrue(queryString.contains("AttendanceStatus.C_WORK"), "Must aggregate C_WORK status");
        assertTrue(queryString.contains("COUNT(DISTINCT l.lectureDate)"), "Must count distinct lecture dates");
        assertTrue(queryString.contains("l.lectureDate >= :startDate"));
        assertTrue(queryString.contains("l.lectureDate <= :endDate"));
    }

    @Test
    @DisplayName("getSubjectTrendsAggregation has proper signature and subject grouping")
    void testGetSubjectTrendsAggregationQuery() throws Exception {
        Method method = AttendanceRepository.class.getMethod("getSubjectTrendsAggregation",
                UUID.class, LocalDate.class, LocalDate.class);
        assertNotNull(method);

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "getSubjectTrendsAggregation must have @Query annotation");
        String queryString = query.value();

        assertTrue(queryString.contains("SELECT l.subjectId"), "Must select subjectId");
        assertTrue(queryString.contains("GROUP BY l.subjectId"), "Must group by subjectId");
        assertTrue(queryString.contains("l.lectureDate >= :startDate"));
        assertTrue(queryString.contains("l.lectureDate <= :endDate"));
    }

    @Test
    @DisplayName("getSubjectMonthlyTrendsAggregation has proper signature and month grouping")
    void testGetSubjectMonthlyTrendsAggregationQuery() throws Exception {
        Method method = AttendanceRepository.class.getMethod("getSubjectMonthlyTrendsAggregation",
                UUID.class, LocalDate.class, LocalDate.class);
        assertNotNull(method);

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "getSubjectMonthlyTrendsAggregation must have @Query annotation");
        String queryString = query.value();

        assertTrue(queryString.contains("YEAR(l.lectureDate)"), "Must extract year");
        assertTrue(queryString.contains("MONTH(l.lectureDate)"), "Must extract month");
        assertTrue(queryString.contains("GROUP BY l.subjectId, YEAR(l.lectureDate), MONTH(l.lectureDate)"),
                "Must group by subject and year/month");
    }
}
