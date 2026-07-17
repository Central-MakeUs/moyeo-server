package com.moyeo.global;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moyeoOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("Moyeo API")
                        .description("CMC Moyeo MVP server API")
                        .version("v1"));
    }

    @Bean
    public OpenApiCustomizer traceIdResponseHeaderCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations()
                .forEach(operation -> operation.getResponses().values().forEach(response -> response.addHeaderObject(
                        TraceIdFilter.HEADER_NAME,
                        new Header()
                                .description("요청 추적 ID입니다. 서버 로그 조회 시 사용합니다.")
                                .schema(new StringSchema().format("uuid"))
                ))));
    }
}
