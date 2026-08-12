package com.quini6.analytics.integration;

import com.quini6.analytics.domain.entity.Usuario;
import com.quini6.analytics.domain.repository.UsuarioRepository;
import com.quini6.analytics.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void login_credencialesValidas_retornaToken() throws Exception {
        Usuario user = new Usuario("testuser", passwordEncoder.encode("pass123"), "USER");
        user.setEnabled(true);
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"pass123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        Usuario user = new Usuario("testuser", passwordEncoder.encode("pass123"), "USER");
        user.setEnabled(true);
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrongpassword\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_usuarioInexistente_retorna401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistent\",\"password\":\"pass\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_usuarioDeshabilitado_retorna401() throws Exception {
        Usuario user = new Usuario("disabled", passwordEncoder.encode("pass123"), "USER");
        user.setEnabled(false);
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"disabled\",\"password\":\"pass123\"}"))
            .andExpect(status().isUnauthorized());
    }
}
