package com.springboot.MessApplication.MessMate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MessApplication.MessMate.config.WebSecurityConfig;
import com.springboot.MessApplication.MessMate.dto.AnnouncementDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.filters.JwtAuthFilter;
import com.springboot.MessApplication.MessMate.services.JwtService;
import com.springboot.MessApplication.MessMate.services.NotificationService;
import com.springboot.MessApplication.MessMate.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({WebSecurityConfig.class, JwtAuthFilter.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow ADMIN to post announcement and return 200 OK with message")
    void shouldAllowAdminToPostAnnouncement() throws Exception {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message("Dinner is ready")
                .notifyAllUsers(true)
                .build();

        when(notificationService.createAnnouncement(any(AnnouncementDto.class)))
                .thenReturn(new SuccessResponseDto("Announcement sent successfully to 10 user(s)"));

        mockMvc.perform(post("/notification/announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Announcement sent successfully to 10 user(s)"));

        verify(notificationService).createAnnouncement(any(AnnouncementDto.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Should deny STUDENT from posting announcement and return 403 Forbidden")
    void shouldDenyStudentFromPostingAnnouncement() throws Exception {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message("Unauthorized message")
                .notifyAllUsers(true)
                .build();

        mockMvc.perform(post("/notification/announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny unauthenticated user from posting announcement and return 401 Unauthorized")
    void shouldDenyUnauthenticatedUserFromPostingAnnouncement() throws Exception {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message("Unauthenticated message")
                .notifyAllUsers(true)
                .build();

        mockMvc.perform(post("/notification/announcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
