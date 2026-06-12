package com.example.jwtapi.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.example.jwtapi.security.RefreshToken;
import com.example.jwtapi.security.RefreshTokenRepository;
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
class AuthIntegrationTests {

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
    void loginWithoutRememberMeReturnsOnlyAccessToken() throws Exception {
        registerUser("login-no-remember@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-no-remember@example.com",
                                  "password": "123456",
                                  "rememberMe": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(nullValue())))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.expiresIn").doesNotExist());

        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    void loginWithRememberMeReturnsAndPersistsRefreshToken() throws Exception {
        registerUser("login-remember@example.com");

        MvcResult result = login("login-remember@example.com", true);
        JsonNode body = json(result);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + body.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login-remember@example.com"));

        String refreshToken = body.get("refreshToken").asText();
        RefreshToken persistedToken = refreshTokenRepository.findAll().getFirst();

        assertEquals(900, body.get("expiresIn").asLong());
        assertEquals(1, refreshTokenRepository.count());
        assertNotEquals(persistedToken.getTokenHash(), refreshToken);
    }

    @Test
    void refreshWithValidTokenRotatesRefreshToken() throws Exception {
        registerUser("refresh-valid@example.com");
        String firstRefreshToken = json(login("refresh-valid@example.com", true)).get("refreshToken").asText();

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(nullValue())))
                .andExpect(jsonPath("$.refreshToken", not(nullValue())))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn();

        String secondRefreshToken = json(result).get("refreshToken").asText();

        assertNotEquals(firstRefreshToken, secondRefreshToken);
        assertEquals(2, refreshTokenRepository.count());
        assertEquals(1, refreshTokenRepository.findAll().stream().filter(RefreshToken::isRevoked).count());
    }

    @Test
    void refreshWithExpiredTokenReturnsUnauthorized() throws Exception {
        registerUser("refresh-expired@example.com");
        String refreshToken = json(login("refresh-expired@example.com", true)).get("refreshToken").asText();
        RefreshToken persistedToken = refreshTokenRepository.findAll().getFirst();
        persistedToken.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(persistedToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithRevokedTokenReturnsUnauthorized() throws Exception {
        registerUser("refresh-revoked@example.com");
        String refreshToken = json(login("refresh-revoked@example.com", true)).get("refreshToken").asText();
        RefreshToken persistedToken = refreshTokenRepository.findAll().getFirst();
        persistedToken.revoke();
        refreshTokenRepository.save(persistedToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        registerUser("logout@example.com");
        String refreshToken = json(login("logout@example.com", true)).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isNoContent());

        assertTrue(refreshTokenRepository.findAll().getFirst().isRevoked());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotatedRefreshTokenCannotBeReused() throws Exception {
        registerUser("rotation@example.com");
        String firstRefreshToken = json(login("rotation@example.com", true)).get("refreshToken").asText();

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String secondRefreshToken = json(result).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(secondRefreshToken)))
                .andExpect(status().isOk());
    }

    @Test
    void generatedAccessTokenAuthenticatesProtectedEndpoint() throws Exception {
        registerUser("access-token@example.com");
        String accessToken = json(login("access-token@example.com", true)).get("accessToken").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("access-token@example.com"));
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

    private MvcResult login(String email, boolean rememberMe) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "123456",
                                  "rememberMe": %s
                                }
                                """.formatted(email, rememberMe)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String refreshBody(String refreshToken) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }
}
