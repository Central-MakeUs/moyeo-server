package com.moyeo.global;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import com.moyeo.global.security.CurrentMember;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String DEPARTURE_PLACE_SEARCH_PATH = "/api/departure-places/searches";

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

    @Bean
    public OpenApiCustomizer departurePlaceSearchSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null || openApi.getPaths().get(DEPARTURE_PLACE_SEARCH_PATH) == null) {
                return;
            }
            openApi.getPaths().get(DEPARTURE_PLACE_SEARCH_PATH).getPost().setSecurity(List.of(
                    new SecurityRequirement().addList("bearerAuth"),
                    new SecurityRequirement()
            ));
        };
    }

    @Bean
    public OperationCustomizer onboardingRequiredResponseCustomizer() {
        return (operation, handlerMethod) -> {
            boolean onboardingRequired = java.util.Arrays.stream(handlerMethod.getMethodParameters())
                    .map(parameter -> parameter.getParameterAnnotation(CurrentMember.class))
                    .anyMatch(annotation -> annotation != null && annotation.onboardingRequired());
            if (!onboardingRequired || operation.getResponses().containsKey("403")) {
                return operation;
            }

            operation.getResponses().addApiResponse("403", new ApiResponse()
                    .description("닉네임 온보딩 미완료")
                    .content(new Content().addMediaType(
                            "application/problem+json",
                            new MediaType().example(Map.of(
                                    "code", "ONBOARDING_REQUIRED",
                                    "status", 403
                            ))
                    )));
            return operation;
        };
    }
}
