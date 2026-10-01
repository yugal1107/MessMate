package com.springboot.MessApplication.MessMate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MessApplication.MessMate.config.WebSecurityConfig;
import com.springboot.MessApplication.MessMate.filters.JwtAuthFilter;
import com.springboot.MessApplication.MessMate.services.AuthService;
import com.springboot.MessApplication.MessMate.services.EmailVerificationService;
import com.springboot.MessApplication.MessMate.services.JwtService;
import com.springboot.MessApplication.MessMate.services.PasswordResetTokenService;
import com.springboot.MessApplication.MessMate.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({WebSecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordResetTokenService passwordResetTokenService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Should verify email through the public verification endpoint")
    void shouldVerifyEmail() throws Exception {
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", "verification-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Email verified successfully")));

        verify(emailVerificationService).verifyEmail("verification-token");
    }

    @Test
    @DisplayName("Should reject invalid signup fields with field-level validation errors")
    void shouldRejectInvalidSignupFields() throws Exception {
        Map<String, Object> invalidSignup = Map.of(
                "email", "not-an-email",
                "password", "123",
                "name", "",
                "contact", "invalid-contact",
                "address", "Hostel"
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSignup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.exception").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("email: Please provide a valid email address"),
                        org.hamcrest.Matchers.containsString("password: Password must be at least 6 characters"),
                        org.hamcrest.Matchers.containsString("name: Name is required"),
                        org.hamcrest.Matchers.containsString("contact: Contact number format is invalid")
                )));
    }
}
