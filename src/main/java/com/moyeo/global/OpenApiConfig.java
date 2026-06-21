package com.moyeo.global;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moyeoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Moyeo API")
                        .description("CMC Moyeo MVP server API")
                        .version("v1"));
    }
}

