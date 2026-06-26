package com.moyeo.global.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(GlobalExceptionHandlerTest.ErrorTestController.class)
class GlobalExceptionHandlerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAllValidationErrorsWithoutRejectedValues() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "capacity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:moyeo:problem:validation-failed"))
                .andExpect(jsonPath("$.title").value("요청 검증 실패"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.instance").value("/test/errors/validation"))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("capacity"))
                .andExpect(jsonPath("$.errors[0].reason").value("인원은 1명 이상이어야 합니다."))
                .andExpect(jsonPath("$.errors[1].field").value("name"))
                .andExpect(jsonPath("$.errors[1].reason").value("이름은 필수입니다."))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
    }

    @Test
    void handlesMethodParameterValidation() throws Exception {
        mockMvc.perform(get("/test/errors/parameter-validation").param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].reason").value("개수는 1 이상이어야 합니다."));
    }

    @Test
    void handlesMalformedJson() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_MALFORMED_REQUEST"));
    }

    @Test
    void handlesMissingRequestParameter() throws Exception {
        mockMvc.perform(get("/test/errors/required"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_MISSING_REQUIRED_VALUE"));
    }

    @Test
    void handlesMissingRequestHeader() throws Exception {
        mockMvc.perform(get("/test/errors/header"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_MISSING_REQUIRED_VALUE"));
    }

    @Test
    void handlesTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/errors/number").param("value", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_TYPE_MISMATCH"));
    }

    @Test
    void handlesUnsupportedHttpMethodAndPreservesAllowHeader() throws Exception {
        mockMvc.perform(post("/test/errors/method"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("COMMON_METHOD_NOT_ALLOWED"));
    }

    @Test
    void handlesUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/test/errors/media")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void handlesUnknownEndpoint() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_ENDPOINT_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/test/errors/not-found"));
    }

    @Test
    void handlesMoyeoException() throws Exception {
        mockMvc.perform(get("/test/errors/custom"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
    }

    @Test
    void masksUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret-internal-message"))));
    }

    @Test
    void leavesHealthResponseUnwrapped() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist());
    }

    @RestController
    @RequestMapping("/test/errors")
    static class ErrorTestController {

        @PostMapping(path = "/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
        ValidationRequest validate(@Valid @RequestBody ValidationRequest request) {
            return request;
        }

        @GetMapping("/parameter-validation")
        int validateParameter(@RequestParam @Min(value = 1, message = "개수는 1 이상이어야 합니다.") int count) {
            return count;
        }

        @GetMapping("/required")
        String required(@RequestParam String value) {
            return value;
        }

        @GetMapping("/header")
        String header(@RequestHeader("X-Test-Header") String value) {
            return value;
        }

        @GetMapping("/number")
        int number(@RequestParam int value) {
            return value;
        }

        @GetMapping("/method")
        String method() {
            return "ok";
        }

        @PostMapping(path = "/media", consumes = MediaType.APPLICATION_JSON_VALUE)
        String media(@RequestBody String value) {
            return value;
        }

        @GetMapping("/custom")
        void custom() {
            throw new MoyeoException(CommonErrorCode.INVALID_REQUEST);
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret-internal-message");
        }
    }

    record ValidationRequest(
            @NotBlank(message = "이름은 필수입니다.") String name,
            @Min(value = 1, message = "인원은 1명 이상이어야 합니다.") int capacity
    ) {
    }
}
