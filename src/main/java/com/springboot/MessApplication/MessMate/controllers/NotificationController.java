package com.springboot.MessApplication.MessMate.controllers;

import com.springboot.MessApplication.MessMate.dto.AnnouncementDto;
import com.springboot.MessApplication.MessMate.dto.NotificationDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications(@RequestParam(value="type", required = false) NotificationType type){
        return ResponseEntity.ok(notificationService.getAllNotifications(type));
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/announcement")
    public ResponseEntity<SuccessResponseDto> createAnnouncement(@RequestBody AnnouncementDto dto) {
        return ResponseEntity.ok(notificationService.createAnnouncement(dto));
    }
}
