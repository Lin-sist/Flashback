package com.flashback;

import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InfrastructureSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private Clock clock;

    @Test
    void contextLoads() {
        assertThat(clock).isNotNull();
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturn403WhenUserAccessAdminApi() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(1001L, AuthRole.USER));

        mockMvc.perform(get("/admin/test/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturn400WhenValidationFailed() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(1002L, AuthRole.USER));

        mockMvc.perform(post("/api/test/validation")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldGenerateAndParseJwtWithUserIdAndRole() {
        AuthUser source = new AuthUser(2001L, AuthRole.ADMIN);
        String token = jwtTokenProvider.createToken(source);

        AuthUser parsed = jwtTokenProvider.parseAuthUser(token);

        assertThat(parsed.getUserId()).isEqualTo(2001L);
        assertThat(parsed.getRole()).isEqualTo(AuthRole.ADMIN);
    }

    @Test
    void shouldNotReturn401ForOptionsPreflight() throws Exception {
        mockMvc.perform(options("/api/test/protected"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void shouldAllowAdminRoleAccessAdminApi() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(3001L, AuthRole.ADMIN));

        mockMvc.perform(get("/admin/test/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void shouldResolveCurrentUserFromArgumentResolver() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(4001L, AuthRole.USER));

        mockMvc.perform(get("/api/test/current-user")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(4001))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void shouldAllowConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/test/protected")
                .header("Origin", "http://frontend.test:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://frontend.test:5173"));
    }

    @Test
    void shouldReturn400WhenPageQueryInvalid() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));

        mockMvc.perform(get("/api/test/page")
                .header("Authorization", "Bearer " + token)
                .param("pageNum", "0")
                .param("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("pageNum")));
    }

    @Test
    void shouldReturn200WhenPageQueryValid() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5002L, AuthRole.USER));

        mockMvc.perform(get("/api/test/page")
                .header("Authorization", "Bearer " + token)
                .param("pageNum", "2")
                .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }
}
