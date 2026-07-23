package com.moyeo.controller.place;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.controller.TestMemberFactory;
import com.moyeo.departure.DeparturePlaceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MyPlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestMemberFactory testMemberFactory;

    @Test
    void savedPlaceApisRequireMemberAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/places"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void memberCanCreateListRenameAndDeleteSavedPlacesWithDuplicates() throws Exception {
        String accessToken = signupAndGetAccessToken("saved-place-crud");

        JsonNode first = savePlace(accessToken, "  회사  ");
        JsonNode duplicate = savePlace(accessToken, "회사 근처");

        mockMvc.perform(get("/api/me/places")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.places[0].id").value(duplicate.get("id").asLong()))
                .andExpect(jsonPath("$.places[1].id").value(first.get("id").asLong()))
                .andExpect(jsonPath("$.places[1].alias").value("회사"));

        mockMvc.perform(patch("/api/me/places/{savedPlaceId}", first.get("id").asLong())
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("alias", "  새 회사  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("새 회사"))
                .andExpect(jsonPath("$.displayName").value("강남파이낸스센터"))
                .andExpect(jsonPath("$.latitude").value(37.500028))
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        mockMvc.perform(delete("/api/me/places/{savedPlaceId}", first.get("id").asLong())
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/me/places")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].id").value(duplicate.get("id").asLong()));
    }

    @Test
    void memberCannotRenameOrDeleteAnotherMembersSavedPlace() throws Exception {
        String ownerToken = signupAndGetAccessToken("saved-place-owner");
        String otherToken = signupAndGetAccessToken("saved-place-other");
        long savedPlaceId = savePlace(ownerToken, "집").get("id").asLong();

        mockMvc.perform(patch("/api/me/places/{savedPlaceId}", savedPlaceId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("alias", "침범"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAVED_PLACE_NOT_FOUND"));

        mockMvc.perform(delete("/api/me/places/{savedPlaceId}", savedPlaceId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAVED_PLACE_NOT_FOUND"));

        mockMvc.perform(get("/api/me/places")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].id").value(savedPlaceId));
    }

    @Test
    void createAndRenameValidatePlaceInput() throws Exception {
        String accessToken = signupAndGetAccessToken("saved-place-validation");

        SavePlaceRequest invalidRequest = new SavePlaceRequest(
                "",
                DeparturePlaceType.PLACE,
                "강남파이낸스센터",
                "서울 강남구 테헤란로 152",
                null,
                null,
                BigDecimal.valueOf(91),
                BigDecimal.valueOf(127.036502)
        );
        mockMvc.perform(post("/api/me/places")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));

        long savedPlaceId = savePlace(accessToken, "집").get("id").asLong();
        mockMvc.perform(patch("/api/me/places/{savedPlaceId}", savedPlaceId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void openApiPublishesSavedPlaceCrudContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/me/places']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/me/places']['post']['description']",
                        containsString("중복 저장을 허용")))
                .andExpect(jsonPath("$['paths']['/api/me/places']['post']['description']",
                        containsString("원본 검색 결과")))
                .andExpect(jsonPath("$['paths']['/api/me/places']['get']['description']",
                        containsString("저장 시각이 같으면")))
                .andExpect(jsonPath("$['paths']['/api/me/places/{savedPlaceId}']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/me/places/{savedPlaceId}']['delete']").exists())
                .andExpect(jsonPath("$['paths']['/api/me/places']['get']['security'][0]['bearerAuth']").exists())
                .andExpect(jsonPath("$['components']['schemas']['SavePlaceRequest']['required']",
                        hasItem("alias")))
                .andExpect(jsonPath("$['components']['schemas']['SavePlaceRequest']['properties']['type']['description']",
                        containsString("STATION")));
    }

    private JsonNode savePlace(String accessToken, String alias) throws Exception {
        SavePlaceRequest request = new SavePlaceRequest(
                alias,
                DeparturePlaceType.PLACE,
                "강남파이낸스센터",
                "서울 강남구 테헤란로 152",
                "서울 강남구 테헤란로 152",
                "서울 강남구 역삼동 737",
                new BigDecimal("37.500028"),
                new BigDecimal("127.036502")
        );
        String response = mockMvc.perform(post("/api/me/places")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias").value(alias.strip()))
                .andExpect(jsonPath("$.type").value("PLACE"))
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private String signupAndGetAccessToken(String loginId) throws Exception {
        return testMemberFactory.createAccessToken(loginId);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
