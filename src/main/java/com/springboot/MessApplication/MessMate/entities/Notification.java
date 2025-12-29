package com.springboot.MessApplication.MessMate.entities;


import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationType type; // e.g., SUBSCRIPTION_EXPIRY, MEAL_UPDATE

    private String message;

    @Column(nullable = false)
    private Boolean isRead =  false;

    @CreationTimestamp
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY) //fetch type is eager by default in many to one
    private User user;
}
