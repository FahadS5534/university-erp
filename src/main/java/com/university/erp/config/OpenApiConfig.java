package com.university.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI universityErpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("University ERP System API")
                        .description(
                                "REST API documentation for the University ERP System. "
                                + "Includes Department and Faculty management."
                        )
                        .version("1.0.0"));
    }
}