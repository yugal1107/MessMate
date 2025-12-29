package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.SubscriptionDto;
import com.springboot.MessApplication.MessMate.entities.MealOff;
import com.springboot.MessApplication.MessMate.entities.Subscription;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionType;
import com.springboot.MessApplication.MessMate.exceptions.ResourceNotFoundException;
import com.springboot.MessApplication.MessMate.exceptions.UserNotSubscribedException;
import com.springboot.MessApplication.MessMate.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    public SubscriptionDto getSubscriptionDetails() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Subscription subscription = getSubscriptionByUserId(user.getId());
        return modelMapper.map(subscription, SubscriptionDto.class);
    }

    public SubscriptionDto requestNewSubscription(SubscriptionType type) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Subscription subscription = getSubscriptionByUserId(user.getId());
        if(subscription.getStatus()==SubscriptionStatus.ACTIVE){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription is already active");
        }
        if(subscription.getStatus()==SubscriptionStatus.REQUESTED){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription already requested");
        }
        subscription.setStatus(SubscriptionStatus.REQUESTED);
        subscription.setType(type);
        subscription.setDate(LocalDateTime.now());
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        return modelMapper.map(savedSubscription, SubscriptionDto.class);
    }


    public SubscriptionDto acceptSubscriptionRequestByUserId(Long id) {
        User user = userService.getUserById(id);
        Subscription subscription = getSubscriptionByUserId(user.getId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setDate(LocalDateTime.now());
        subscription.setMeals(56);
        return modelMapper.map(subscriptionRepository.save(subscription), SubscriptionDto.class);
    }

    public SubscriptionDto getSubscriptionDetailsByUserId(long userId) {
        Subscription subscription = getSubscriptionByUserId(userId);
        return modelMapper.map(subscription, SubscriptionDto.class);
    }


    //non-controller methods

    public Subscription getSubscriptionByUserId(Long userId) {
        return subscriptionRepository.findByUser_Id(userId)
                .orElseThrow(() ->new ResourceNotFoundException("User with Id " + userId + " doesnot exist"));
    }

    public Subscription saveSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }
    public void checkSubscriptionStatus(Long userId) {
        Subscription subscription = getSubscriptionByUserId(userId);
        if (subscription==null){
            throw new ResourceNotFoundException("User with Id " + userId + " does not exist");
        }
        if(Set.of(SubscriptionStatus.INACTIVE,SubscriptionStatus.REQUESTED).contains(subscription.getStatus()) ){
            throw new UserNotSubscribedException("User not subscribed");
        }
    }

    public  void updateStatusIfMealsExhausted(Subscription subscription) {
        if (subscription.getMeals() == 0) {
            subscription.setStatus(SubscriptionStatus.INACTIVE);
            subscription.setDate(LocalDateTime.now());
        }
    }

    public List<Subscription> getLunchCountableActiveSubscriptions(){
        return subscriptionRepository.findAllByStatusAndUser_MealOff_Lunch(SubscriptionStatus.ACTIVE,false);
    }

    public List<Subscription> getDinnerCountableActiveSubscriptions(){
        return subscriptionRepository.findAllByStatusAndUser_MealOff_Dinner(SubscriptionStatus.ACTIVE,false);
    }
}
