package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.AnnouncementDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.entities.Notification;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.Role;
import com.springboot.MessApplication.MessMate.exceptions.BadRequestException;
import com.springboot.MessApplication.MessMate.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Should throw BadRequestException when AnnouncementDto is null")
    void shouldThrowBadRequestExceptionWhenDtoIsNull() {
        assertThrows(BadRequestException.class, () -> notificationService.createAnnouncement(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should throw BadRequestException when message is null or blank")
    void shouldThrowBadRequestExceptionWhenMessageIsBlank(String message) {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message(message)
                .notifyAllUsers(true)
                .build();

        assertThrows(BadRequestException.class, () -> notificationService.createAnnouncement(dto));
    }

    @Test
    @DisplayName("Should throw BadRequestException when message exceeds 500 characters")
    void shouldThrowBadRequestExceptionWhenMessageExceedsMaxLength() {
        String longMessage = "a".repeat(501);
        AnnouncementDto dto = AnnouncementDto.builder()
                .message(longMessage)
                .notifyAllUsers(true)
                .build();

        assertThrows(BadRequestException.class, () -> notificationService.createAnnouncement(dto));
    }

    @Test
    @DisplayName("Should throw BadRequestException when notifyAllUsers flag is null")
    void shouldThrowBadRequestExceptionWhenNotifyAllUsersIsNull() {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message("Valid message")
                .notifyAllUsers(null)
                .build();

        assertThrows(BadRequestException.class, () -> notificationService.createAnnouncement(dto));
    }

    @Test
    @DisplayName("Should send announcement to all students when notifyAllUsers is true")
    void shouldSendAnnouncementToAllStudentsWhenNotifyAllUsersIsTrue() {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message("Test announcement message")
                .notifyAllUsers(true)
                .build();

        User student1 = User.builder().id(1L).name("Student One").email("s1@test.com").role(Role.STUDENT).build();
        User student2 = User.builder().id(2L).name("Student Two").email("s2@test.com").role(Role.STUDENT).build();
        List<User> students = List.of(student1, student2);

        when(userService.getStudents()).thenReturn(students);

        SuccessResponseDto response = notificationService.createAnnouncement(dto);

        assertEquals("Announcement sent successfully to 2 user(s)", response.getMessage());

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("Test announcement message", saved.get(0).getMessage());
        assertEquals(NotificationType.ANNOUNCEMENT, saved.get(0).getType());
        assertEquals(Boolean.FALSE, saved.get(0).getIsRead());
        assertEquals(student1, saved.get(0).getUser());
        assertEquals(student2, saved.get(1).getUser());
    }

    @Test
    @DisplayName("Should send announcement only to active subscribed students when notifyAllUsers is false")
    void shouldSendAnnouncementOnlyToActiveSubscribedStudentsWhenNotifyAllUsersIsFalse() {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message("Subscribers announcement")
                .notifyAllUsers(false)
                .build();

        User activeStudent = User.builder().id(10L).name("Active Student").role(Role.STUDENT).build();
        when(userService.getActiveSubscribedStudents()).thenReturn(List.of(activeStudent));

        SuccessResponseDto response = notificationService.createAnnouncement(dto);

        assertEquals("Announcement sent successfully to 1 user(s)", response.getMessage());

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals("Subscribers announcement", saved.get(0).getMessage());
        assertEquals(activeStudent, saved.get(0).getUser());
    }

    @Test
    @DisplayName("Should return 0 users message and not invoke saveAll when no recipients found")
    void shouldHandleZeroRecipientsGracefullyWhenNoStudentsFound() {
        AnnouncementDto dto = AnnouncementDto.builder()
                .message("Message to empty group")
                .notifyAllUsers(false)
                .build();

        when(userService.getActiveSubscribedStudents()).thenReturn(List.of());

        SuccessResponseDto response = notificationService.createAnnouncement(dto);

        assertEquals("Announcement sent successfully to 0 user(s)", response.getMessage());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should dispatch notification to all admin users")
    void shouldDispatchNotificationToAllAdminUsers() {
        User admin1 = User.builder().id(1L).name("Admin One").role(Role.ADMIN).build();
        User admin2 = User.builder().id(2L).name("Admin Two").role(Role.ADMIN).build();

        when(userService.getAdmins()).thenReturn(List.of(admin1, admin2));

        notificationService.notifyAllAdmins(
                NotificationType.MEAL_OFF,
                "Lunch off for John Doe (ID: 5) was cancelled by admin"
        );

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        assertEquals(2, saved.size());

        assertEquals(admin1, saved.get(0).getUser());
        assertEquals(NotificationType.MEAL_OFF, saved.get(0).getType());
        assertEquals("Lunch off for John Doe (ID: 5) was cancelled by admin", saved.get(0).getMessage());

        assertEquals(admin2, saved.get(1).getUser());
        assertEquals(NotificationType.MEAL_OFF, saved.get(1).getType());
        assertEquals("Lunch off for John Doe (ID: 5) was cancelled by admin", saved.get(1).getMessage());
    }

    @Test
    @DisplayName("Should do nothing and not call saveAll when no admins exist")
    void shouldNotSaveNotificationsWhenNoAdminsExist() {
        when(userService.getAdmins()).thenReturn(List.of());

        notificationService.notifyAllAdmins(
                NotificationType.MEAL_OFF,
                "Dinner off for Jane (ID: 6) was cancelled by admin"
        );

        verify(notificationRepository, never()).saveAll(any());
    }
}

