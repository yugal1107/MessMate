package com.springboot.MessApplication.MessMate.entities;

import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //For non owning side in one to one mapping, the default fetch type (eager) can be changed,
    //but it is not  honored by hibernate
    @OneToOne(mappedBy ="subscription", cascade = CascadeType.ALL)
    private User user;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    private SubscriptionType type;

    private LocalDateTime date;

    private Integer meals;

}
