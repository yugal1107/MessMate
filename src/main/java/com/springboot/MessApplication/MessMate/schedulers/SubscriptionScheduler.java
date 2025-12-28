package com.springboot.MessApplication.MessMate.schedulers;

import com.springboot.MessApplication.MessMate.entities.MealOff;
import com.springboot.MessApplication.MessMate.entities.Subscription;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.Meal;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final UserService userService;
    private final MealOffService mealOffService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final SubscriptionMealOffCoordinatorService subscriptionMealOffCoordinatorService;

    @Scheduled(cron = "0 0 16 * * *",zone = "Asia/Kolkata")
    void countLunch() {
        subscriptionMealOffCoordinatorService.countMeal(Meal.LUNCH);
        mealOffService.resetLunchOffs();
    }

    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Kolkata")
    void countDinner() {
        subscriptionMealOffCoordinatorService.countMeal(Meal.DINNER);
        mealOffService.resetDinnerOffs();
    }

}
