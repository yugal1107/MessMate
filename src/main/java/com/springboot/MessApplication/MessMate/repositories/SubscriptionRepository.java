package com.springboot.MessApplication.MessMate.repositories;

import com.springboot.MessApplication.MessMate.entities.Subscription;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {

    List<Subscription> findByStatus(SubscriptionStatus status);

    Subscription findByUser(User user);

    Optional<Subscription> findByUser_Id(long userId);

    List<Subscription> findAllByStatusAndUser_MealOff_Lunch(SubscriptionStatus subscriptionStatus, Boolean lunch);

    List<Subscription> findAllByStatusAndUser_MealOff_Dinner(SubscriptionStatus subscriptionStatus, Boolean dinner);
}
