package com.springboot.MessApplication.MessMate.repositories;

import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.Role;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(java.lang.String email);

    List<User> findBySubscription_Status(SubscriptionStatus status);

    List<User> findByMealOff_Lunch(Boolean mealOffLunch);

    List<User> findByMealOff_Dinner(Boolean mealOffDinner);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findBySubscription_statusAndSubscription_type(SubscriptionStatus status, SubscriptionType type);

    List<User> findByRole(Role role);

    List<User> findByRoleAndSubscription_Status(Role role, SubscriptionStatus status);
}
