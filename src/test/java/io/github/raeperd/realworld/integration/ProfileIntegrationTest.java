package io.github.raeperd.realworld.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.raeperd.realworld.application.user.UserLoginRequestDTO;
import io.github.raeperd.realworld.application.user.UserPostRequestDTO;
import io.github.raeperd.realworld.domain.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@AutoConfigureMockMvc
@TestInstance(PER_CLASS)
@SpringBootTest
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final User savedUser = new User("raeperd@gmail.com", "raeperd", "password");
    private final User celebrity = new User("celeb@gmail.com", "celebrity", "psasword");

    @BeforeAll
    void initializeUser() throws Exception {
        saveUser(savedUser);
        saveUser(celebrity);
    }

    private void saveUser(User user) throws Exception {
        final var userPostRequestDTO = new UserPostRequestDTO(user.getUsername(), user.getEmail(), "password");

        mockMvc.perform(post("/users")
                .accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userPostRequestDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void when_get_profile_expect_return_profile() throws Exception {
        final var token = loginAndRememberToken();

        mockMvc.perform(get("/profiles/{username}", savedUser.getUsername())
                .header(AUTHORIZATION, "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("profile").exists())
                .andExpect(jsonPath("profile.username", is(savedUser.getUsername())))
                .andExpect(jsonPath("profile.bio").hasJsonPath())
                .andExpect(jsonPath("profile.image").hasJsonPath())
                .andExpect(jsonPath("profile.following").isBoolean());
    }

    @Test
    void when_follow_user_expect_return_following_profile() throws Exception {
        final var token = loginAndRememberToken();

        mockMvc.perform(post("/profiles/{username}/follow", celebrity.getUsername())
                .header(AUTHORIZATION, "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("profile").exists())
                .andExpect(jsonPath("profile.following", is(true)));

        mockMvc.perform(get("/profiles/{username}", celebrity.getUsername())
                .header(AUTHORIZATION, "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("profile").exists())
                .andExpect(jsonPath("profile.following", is(true)));

        mockMvc.perform(delete("/profiles/{username}/unfollow", celebrity.getUsername())
                .header(AUTHORIZATION, "Token " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("profile").exists())
                .andExpect(jsonPath("profile.following", is(false)));
    }

    private ResultActions login() throws Exception {
        final var userLoginRequestDTO = new UserLoginRequestDTO(savedUser.getEmail(), "password");
        return mockMvc.perform(post("/users/login")
                .accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequestDTO)));
    }

    private String loginAndRememberToken() throws Exception {
        final var responseBodyAsString = login()
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBodyAsString)
                .get("user").get("token").textValue();
    }

}