package com.moyeo.controller.feedback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.controller.TestMemberFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class FeedbackControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestMemberFactory testMemberFactory;

    @Test
    void feedbackApisRequireCompletedMemberAuthentication() throws Exception {
        mockMvc.perform(get("/api/feedbacks"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/api/feedbacks")
                        .header("Authorization", bearer(testMemberFactory.createPendingAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"의견\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void memberCanSubmitAndViewOnlyOwnFeedbackHistory() throws Exception {
        String memberToken = testMemberFactory.createAccessToken("feedback-member");
        String otherMemberToken = testMemberFactory.createAccessToken("feedback-other");
        JsonNode first = submit(memberToken, "  첫 번째 의견  ");
        JsonNode second = submit(memberToken, "두 번째 의견");
        submit(otherMemberToken, "다른 사용자의 의견");

        mockMvc.perform(get("/api/feedbacks").header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbacks.length()").value(2))
                .andExpect(jsonPath("$.feedbacks[0].id").value(second.get("id").asLong()))
                .andExpect(jsonPath("$.feedbacks[0].content").value("두 번째 의견"))
                .andExpect(jsonPath("$.feedbacks[1].id").value(first.get("id").asLong()))
                .andExpect(jsonPath("$.feedbacks[1].content").value("첫 번째 의견"))
                .andExpect(jsonPath("$.feedbacks[0].createdAt").exists());
    }

    @Test
    void feedbackContentMustNotBeBlankOrLongerThanThousandCharacters() throws Exception {
        String memberToken = testMemberFactory.createAccessToken("feedback-validation");
        mockMvc.perform(post("/api/feedbacks").header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
        mockMvc.perform(post("/api/feedbacks").header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "a".repeat(1001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void openApiPublishesFeedbackContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/feedbacks']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/feedbacks']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/feedbacks']['post']['security'][0]['bearerAuth']").exists())
                .andExpect(jsonPath("$['components']['schemas']['CreateFeedbackRequest']['required']", hasItem("content")));
    }

    private JsonNode submit(String accessToken, String content) throws Exception {
        String response = mockMvc.perform(post("/api/feedbacks").header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(content.strip()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String bearer(String accessToken) { return "Bearer " + accessToken; }
}
