package com.springboot.MessApplication.MessMate.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MessApplication.MessMate.config.WebSecurityConfig;
import com.springboot.MessApplication.MessMate.dto.ChangePasswordRequestDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.dto.UpdateProfileDto;
import com.springboot.MessApplication.MessMate.dto.UserDto;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({WebSecurityConfig.class, JwtAuthFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Should allow authenticated student to update their own profile via PUT /user")
    void shouldAllowAuthenticatedStudentToUpdateOwnProfile() throws Exception {
        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name("Updated Student")
                .contact("9999888877")
                .address("Hostel Room 101")
                .build();

        UserDto expectedUserDto = new UserDto();
        expectedUserDto.setId(1L);
        expectedUserDto.setName("Updated Student");
        expectedUserDto.setEmail("student@test.com");
        expectedUserDto.setContact("9999888877");
        expectedUserDto.setAddress("Hostel Room 101");

        when(userService.updateMyProfile(any(UpdateProfileDto.class))).thenReturn(expectedUserDto);

        mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Student"))
                .andExpect(jsonPath("$.data.contact").value("9999888877"))
                .andExpect(jsonPath("$.data.address").value("Hostel Room 101"));

        verify(userService).updateMyProfile(any(UpdateProfileDto.class));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for PUT /user when unauthenticated")
    void shouldReturn401WhenUpdatingProfileUnauthenticated() throws Exception {
        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name("Anonymous")
                .build();

        mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow ADMIN to update user profile by ID via PUT /user/{id}")
    void shouldAllowAdminToUpdateUserProfileById() throws Exception {
        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name("Admin Modified Name")
                .contact("1234567890")
                .address("Block B")
                .build();

        UserDto expectedUserDto = new UserDto();
        expectedUserDto.setId(2L);
        expectedUserDto.setName("Admin Modified Name");
        expectedUserDto.setEmail("target@test.com");
        expectedUserDto.setContact("1234567890");
        expectedUserDto.setAddress("Block B");

        when(userService.updateUserProfileById(eq(2L), any(UpdateProfileDto.class))).thenReturn(expectedUserDto);

        mockMvc.perform(put("/user/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.name").value("Admin Modified Name"));

        verify(userService).updateUserProfileById(eq(2L), any(UpdateProfileDto.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Should deny STUDENT from updating user profile by ID via PUT /user/{id} (403 Forbidden)")
    void shouldDenyStudentFromUpdatingUserProfileById() throws Exception {
        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name("Hacker")
                .build();

        mockMvc.perform(put("/user/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Should allow authenticated user to change password via PUT /user/change-password")
    void shouldAllowAuthenticatedUserToChangePassword() throws Exception {
        ChangePasswordRequestDto dto = ChangePasswordRequestDto.builder()
                .currentPassword("oldPass123")
                .newPassword("newPass456")
                .build();

        when(userService.changePassword(any(ChangePasswordRequestDto.class)))
                .thenReturn(new SuccessResponseDto("Password changed successfully"));

        mockMvc.perform(put("/user/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Password changed successfully"));

        verify(userService).changePassword(any(ChangePasswordRequestDto.class));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for PUT /user/change-password when unauthenticated")
    void shouldReturn401WhenChangingPasswordUnauthenticated() throws Exception {
        ChangePasswordRequestDto dto = ChangePasswordRequestDto.builder()
                .currentPassword("oldPass123")
                .newPassword("newPass456")
                .build();

        mockMvc.perform(put("/user/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
