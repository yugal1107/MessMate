package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.AnnouncementDto;
import com.springboot.MessApplication.MessMate.dto.NotificationDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.entities.Notification;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.exceptions.BadRequestException;
import com.springboot.MessApplication.MessMate.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ModelMapper modelMapper;
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public List<NotificationDto> getAllNotifications(NotificationType type) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Notification> notifications;
        if(type==null){
            notifications = notificationRepository.findByUserOrderByIsReadAscTimestampDesc(user);
        }else{
            notifications = notificationRepository.findByUserAndTypeOrderByIsReadAscTimestampDesc(user,type);
        }


        List<NotificationDto> notificationDtoList = notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationDto.class))
                .toList();
        //mark unread notifications as read
        List<Notification> unreadNotifications = notifications
                .stream()
                .filter(notification -> !notification.getIsRead())
                .peek(notification -> notification.setIsRead(true))
                .toList();

        //save them
        notificationRepository.saveAll(unreadNotifications);

        return notificationDtoList;
    }

    public void createNotification(Long userId, NotificationType type, String message) {
        User user =  userService.getUserById(userId);
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Transactional
    public SuccessResponseDto createAnnouncement(AnnouncementDto dto) {
        if (dto == null) {
            throw new BadRequestException("Announcement payload cannot be null");
        }
        if (dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            throw new BadRequestException("Announcement message cannot be empty");
        }
        String trimmedMessage = dto.getMessage().trim();
        if (trimmedMessage.length() > 500) {
            throw new BadRequestException("Announcement message cannot exceed 500 characters");
        }
        if (dto.getNotifyAllUsers() == null) {
            throw new BadRequestException("notifyAllUsers flag is required");
        }

        List<User> recipients = Boolean.TRUE.equals(dto.getNotifyAllUsers())
                ? userService.getStudents()
                : userService.getActiveSubscribedStudents();

        List<Notification> notifications = recipients.stream()
                .map(user -> Notification.builder()
                        .user(user)
                        .type(NotificationType.ANNOUNCEMENT)
                        .message(trimmedMessage)
                        .isRead(false)
                        .build())
                .toList();

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }

        return new SuccessResponseDto("Announcement sent successfully to " + recipients.size() + " user(s)");
    }
}
