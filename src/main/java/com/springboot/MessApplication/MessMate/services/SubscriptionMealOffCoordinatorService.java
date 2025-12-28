package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.SubscriptionDto;
import com.springboot.MessApplication.MessMate.entities.Subscription;
import com.springboot.MessApplication.MessMate.entities.enums.Meal;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SubscriptionMealOffCoordinatorService {

    private final SubscriptionService subscriptionService;
    private final MealOffService mealOffService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    @Transactional
    public SubscriptionDto updateSubscriptionByUserId(long userId, SubscriptionDto subscriptionDto) {
        if(subscriptionDto.getMeals()<0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meal count cannot be less than 0");
        }
        Subscription subscription = subscriptionService.getSubscriptionByUserId(userId);
        subscription.setMeals(subscriptionDto.getMeals());
        subscriptionService.updateStatusIfMealsExhausted(subscription);

        Subscription savedSubscription = subscriptionService.saveSubscription(subscription);
        notificationService.createNotification(
                userId,
                NotificationType.MEAL_UPDATE,
                "Your meal count has been updated to "+ savedSubscription.getMeals()+" by admin"
        );
        if(savedSubscription.getStatus().equals(SubscriptionStatus.INACTIVE)){
            notificationService.createNotification(
                    userId,
                    NotificationType.SUBSCRIPTION_EXPIRY,
                    "Your subscription has been expired (via admin) "
            );
            mealOffService.resetMealOff(userId);
        }
        return modelMapper.map(savedSubscription,SubscriptionDto.class);
    }

    @Transactional
    public  void countMeal(Meal meal){
        List<Subscription> subscriptions =
                meal == Meal.LUNCH
                        ? subscriptionService.getLunchCountableActiveSubscriptions()
                        : subscriptionService.getDinnerCountableActiveSubscriptions();
        for(Subscription subscription : subscriptions){

            subscription.setMeals(subscription.getMeals()-1);
            subscriptionService.updateStatusIfMealsExhausted(subscription);

            Subscription savedSubscription = subscriptionService.saveSubscription(subscription);

            //since fetch type is eager for User in subscription, we can directly get user from subscription object
            //we do not need to make the database call
            Long userId = subscription.getUser().getId();

            notificationService.createNotification(
                    userId,NotificationType.MEAL_UPDATE,"Your " + meal + " counted successfully for " + LocalDate.now()
            );
            //subscription expiry check
            if(savedSubscription.getStatus().equals(SubscriptionStatus.INACTIVE)){
                notificationService.createNotification(
                        userId,
                        NotificationType.SUBSCRIPTION_EXPIRY,
                        "Your subscription has been expired "
                );
                mealOffService.resetMealOff(userId);
            }
        }
    }


}
