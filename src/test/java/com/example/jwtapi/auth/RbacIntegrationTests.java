package com.example.jwtapi.auth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.jwtapi.security.RefreshTokenRepository;
import com.example.jwtapi.user.Role;
import com.example.jwtapi.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTests {

    private static final String BOOTSTRAP_TOKEN = "change-me-dev-bootstrap-token";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void publicRegisterCreatesUserRole() throws Exception {
        registerUser("user-role@example.com");

        assertEquals(Role.USER, userRepository.findByEmail("user-role@example.com").orElseThrow().getRole());
    }

    @Test
    void bootstrapCreatesFirstAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bootstrapBody("admin@example.com", BOOTSTRAP_TOKEN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertEquals(Role.ADMIN, userRepository.findByEmail("admin@example.com").orElseThrow().getRole());
    }

    @Test
    void bootstrapRejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/api/admin/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bootstrapBody("admin@example.com", "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapRejectsSecondAdmin() throws Exception {
        bootstrapAdmin("admin@example.com");

        mockMvc.perform(post("/api/admin/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bootstrapBody("admin2@example.com", BOOTSTRAP_TOKEN)))
                .andExpect(status().isConflict());
    }

    @Test
    void userCannotAccessAdminEndpoints() throws Exception {
        registerUser("regular@example.com");
        String accessToken = login("regular@example.com").get("accessToken").asText();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        bootstrapAdmin("admin@example.com");
        registerUser("regular@example.com");
        String accessToken = login("admin@example.com").get("accessToken").asText();

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void adminCanUpdateUserRole() throws Exception {
        bootstrapAdmin("admin@example.com");
        registerUser("regular@example.com");
        Long regularUserId = userRepository.findByEmail("regular@example.com").orElseThrow().getId();
        String accessToken = login("admin@example.com").get("accessToken").asText();

        mockMvc.perform(patch("/api/admin/users/{userId}/role", regularUserId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertEquals(Role.ADMIN, userRepository.findByEmail("regular@example.com").orElseThrow().getRole());
    }

    @Test
    void adminLoginReturnsAccessTokenWithRole() throws Exception {
        bootstrapAdmin("admin@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.com",
                                  "password": "123456",
                                  "rememberMe": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(nullValue())));
    }

    private void registerUser(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Usuario Teste",
                                  "email": "%s",
                                  "password": "123456"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private void bootstrapAdmin(String email) throws Exception {
        mockMvc.perform(post("/api/admin/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bootstrapBody(email, BOOTSTRAP_TOKEN)))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "123456",
                                  "rememberMe": false
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bootstrapBody(String email, String token) {
        return """
                {
                  "name": "Administrador",
                  "email": "%s",
                  "password": "123456",
                  "bootstrapToken": "%s"
                }
                """.formatted(email, token);
    }
}
