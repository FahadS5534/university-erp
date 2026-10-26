package com.university.erp.common;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public GroupedOpenApi studentEnrollmentApi() {
        return GroupedOpenApi.builder()
                .group("students-enrollments")
                .pathsToMatch("/api/students/**", "/api/enrollments/**")
                .build();
    }
}
