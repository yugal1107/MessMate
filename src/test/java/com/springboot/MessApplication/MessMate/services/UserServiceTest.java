package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.ChangePasswordRequestDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.dto.UpdateProfileDto;
import com.springboot.MessApplication.MessMate.dto.UserDto;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.Role;
import com.springboot.MessApplication.MessMate.exceptions.BadRequestException;
import com.springboot.MessApplication.MessMate.exceptions.ResourceNotFoundException;
import com.springboot.MessApplication.MessMate.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User authUser;

    @BeforeEach
    void setUp() {
        authUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .name("Original Name")
                .contact("1234567890")
                .address("Old Address")
                .role(Role.STUDENT)
                .password("encodedPassword")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(User user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should throw BadRequestException when updateMyProfile has null DTO")
    void shouldThrowBadRequestWhenUpdateMyProfileDtoIsNull() {
        mockSecurityContext(authUser);
        assertThrows(BadRequestException.class, () -> userService.updateMyProfile(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should throw BadRequestException when updateMyProfile has blank name")
    void shouldThrowBadRequestWhenNameIsBlank(String blankName) {
        mockSecurityContext(authUser);
        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name(blankName)
                .contact("9999999999")
                .address("New Address")
                .build();

        assertThrows(BadRequestException.class, () -> userService.updateMyProfile(dto));
    }

    @Test
    @DisplayName("Should update current user profile and save")
    void shouldUpdateMyProfileSuccessfully() {
        mockSecurityContext(authUser);

        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name("New Name")
                .contact("9876543210")
                .address("Updated Address, City")
                .build();

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.updateMyProfile(dto);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("9876543210", result.getContact());
        assertEquals("Updated Address, City", result.getAddress());
        assertEquals(authUser.getEmail(), result.getEmail());

        verify(userRepository).save(authUser);
        assertEquals("New Name", authUser.getName());
        assertEquals("9876543210", authUser.getContact());
        assertEquals("Updated Address, City", authUser.getAddress());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when admin updates non-existent user")
    void shouldThrowResourceNotFoundWhenAdminUpdatesNonExistentUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name("Admin Edited Name")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserProfileById(99L, dto));
    }

    @Test
    @DisplayName("Should update user by id and send ADMIN_UPDATE notification")
    void shouldUpdateUserProfileByIdAndNotifyUser() {
        User targetUser = User.builder()
                .id(2L)
                .email("student@test.com")
                .name("Target Name")
                .contact("1111111111")
                .address("Target Address")
                .role(Role.STUDENT)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileDto dto = UpdateProfileDto.builder()
                .name("Admin Changed Name")
                .contact("2222222222")
                .address("Admin Changed Address")
                .build();

        UserDto result = userService.updateUserProfileById(2L, dto);

        assertNotNull(result);
        assertEquals("Admin Changed Name", result.getName());
        assertEquals("2222222222", result.getContact());
        assertEquals("Admin Changed Address", result.getAddress());

        verify(userRepository).save(targetUser);
        verify(notificationService).createNotification(
                eq(2L),
                eq(NotificationType.ADMIN_UPDATE),
                contains("updated by admin")
        );
    }

    @Test
    @DisplayName("Should throw BadRequestException when ChangePasswordRequestDto is null")
    void shouldThrowBadRequestWhenChangePasswordDtoIsNull() {
        mockSecurityContext(authUser);
        assertThrows(BadRequestException.class, () -> userService.changePassword(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Should throw BadRequestException when current or new password is blank")
    void shouldThrowBadRequestWhenPasswordsAreBlank(String blankValue) {
        mockSecurityContext(authUser);

        ChangePasswordRequestDto dto1 = ChangePasswordRequestDto.builder()
                .currentPassword(blankValue)
                .newPassword("validNewPass")
                .build();
        assertThrows(BadRequestException.class, () -> userService.changePassword(dto1));

        ChangePasswordRequestDto dto2 = ChangePasswordRequestDto.builder()
                .currentPassword("validOldPass")
                .newPassword(blankValue)
                .build();
        assertThrows(BadRequestException.class, () -> userService.changePassword(dto2));
    }

    @Test
    @DisplayName("Should throw BadRequestException when new password is less than 6 characters")
    void shouldThrowBadRequestWhenNewPasswordIsTooShort() {
        mockSecurityContext(authUser);
        ChangePasswordRequestDto dto = ChangePasswordRequestDto.builder()
                .currentPassword("oldPassword")
                .newPassword("12345")
                .build();

        assertThrows(BadRequestException.class, () -> userService.changePassword(dto));
    }

    @Test
    @DisplayName("Should throw BadRequestException when current password does not match")
    void shouldThrowBadRequestWhenCurrentPasswordDoesNotMatch() {
        mockSecurityContext(authUser);

        ChangePasswordRequestDto dto = ChangePasswordRequestDto.builder()
                .currentPassword("wrongPassword")
                .newPassword("newValidPassword")
                .build();

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("wrongPassword", authUser.getPassword())).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.changePassword(dto));
        assertEquals("Current password does not match", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should change password successfully when current password is valid")
    void shouldChangePasswordSuccessfully() {
        mockSecurityContext(authUser);

        ChangePasswordRequestDto dto = ChangePasswordRequestDto.builder()
                .currentPassword("correctOldPassword")
                .newPassword("newValidPassword")
                .build();

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("correctOldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newValidPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuccessResponseDto response = userService.changePassword(dto);

        assertNotNull(response);
        assertEquals("Password changed successfully", response.getMessage());
        assertEquals("newEncodedPassword", authUser.getPassword());
        verify(userRepository).save(authUser);
    }
}
