package com.moyeo.controller.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.controller.TestMemberFactory;
import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.domain.place.SavedPlaceCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestMemberFactory testMemberFactory;

    @Test
    void completedMemberCanReadMyPageWithAReplicatedSavedPlaceList() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("mypage");
        savePlace(accessToken, "집");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("mypage"))
                .andExpect(jsonPath("$.profile.type").value("COLOR"))
                .andExpect(jsonPath("$.profile.color").value("GRAY"))
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].category").value("HOME"))
                .andExpect(jsonPath("$.places[0].alias").value("집"));
    }

    @Test
    void myPageRequiresCompletedOnboarding() throws Exception {
        String accessToken = testMemberFactory.createPendingAccessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    private void savePlace(String accessToken, String alias) throws Exception {
        mockMvc.perform(post("/api/me/places")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "alias", alias,
                                "category", SavedPlaceCategory.HOME,
                                "type", DeparturePlaceType.PLACE,
                                "displayName", "강남역",
                                "address", "서울 강남구",
                                "latitude", BigDecimal.valueOf(37.4979),
                                "longitude", BigDecimal.valueOf(127.0276)
                        ))))
                .andExpect(status().isCreated());
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
