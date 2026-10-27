package com.university.erp.common;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public GroupedOpenApi allApis() {
        return group("all-apis", "/api/**");
    }

    @Bean
    public GroupedOpenApi attendanceApi() {
        return group("attendance", "/api/attendance/**");
    }

    @Bean
    public GroupedOpenApi classroomsApi() {
        return group("classrooms", "/api/classrooms/**");
    }

    @Bean
    public GroupedOpenApi coursesApi() {
        return group("courses", "/api/courses/**");
    }

    @Bean
    public GroupedOpenApi departmentsApi() {
        return group("departments", "/api/departments/**");
    }

    @Bean
    public GroupedOpenApi enrollmentsApi() {
        return group("enrollments", "/api/enrollments/**");
    }

    @Bean
    public GroupedOpenApi examinationsApi() {
        return group("examinations", "/api/examinations/**");
    }

    @Bean
    public GroupedOpenApi facultiesApi() {
        return group("faculties", "/api/faculties/**");
    }

    @Bean
    public GroupedOpenApi feesApi() {
        return group("fees", "/api/fees/**");
    }

    @Bean
    public GroupedOpenApi gradesApi() {
        return group("grades", "/api/grades/**");
    }

    @Bean
    public GroupedOpenApi lecturesApi() {
        return group("lectures", "/api/lectures/**");
    }

    @Bean
    public GroupedOpenApi libraryBooksApi() {
        return group("library-books", "/api/library/books/**");
    }

    @Bean
    public GroupedOpenApi libraryTransactionsApi() {
        return group("library-transactions", "/api/library/transactions/**");
    }

    @Bean
    public GroupedOpenApi paymentsApi() {
        return group("payments", "/api/payments/**");
    }

    @Bean
    public GroupedOpenApi studentsApi() {
        return group("students", "/api/students/**");
    }

    @Bean
    public GroupedOpenApi subjectsApi() {
        return group("subjects", "/api/subjects/**");
    }

    @Bean
    public GroupedOpenApi timetablesApi() {
        return group("timetables", "/api/timetables/**");
    }

    @Bean
    public GroupedOpenApi studentEnrollmentApi() {
        return group("students-enrollments", "/api/students/**", "/api/enrollments/**");
    }

    private GroupedOpenApi group(String name, String... paths) {
        return GroupedOpenApi.builder()
                .group(name)
                .pathsToMatch(paths)
                .build();
    }
}
