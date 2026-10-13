package com.dgjs.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dgjsOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("DGJS 2.0 Platform API")
                .description("정형 데이터 적재(엑셀/CSV/수동 스키마) 및 데이터셋 조회·CRUD API")
                .version("v0.0.1"));
    }
}
