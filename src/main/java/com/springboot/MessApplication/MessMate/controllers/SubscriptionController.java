package com.springboot.MessApplication.MessMate.controllers;


import com.springboot.MessApplication.MessMate.dto.SubscriptionDto;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionType;
import com.springboot.MessApplication.MessMate.services.SubscriptionMealOffCoordinatorService;
import com.springboot.MessApplication.MessMate.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionMealOffCoordinatorService subscriptionMealOffCoordinatorService;

    @GetMapping
    public ResponseEntity<SubscriptionDto> getSubscriptionDetails() {
        return ResponseEntity.ok(subscriptionService.getSubscriptionDetails());
    }

    @PostMapping("/request-new-subscription/{type}")
    public ResponseEntity<SubscriptionDto> requestNewSubscription(@PathVariable SubscriptionType type){
        return ResponseEntity.ok(subscriptionService.requestNewSubscription(type));
    }


    @PostMapping("/requests/{userId}")
    public ResponseEntity<SubscriptionDto> acceptSubscriptionRequestByUserId(@PathVariable Long userId){
        return ResponseEntity.ok(subscriptionService.acceptSubscriptionRequestByUserId(userId));
    }

    //TODO getSubscriptionDetailsByUserId
    @GetMapping("/{userId}")
    public ResponseEntity<SubscriptionDto> getSubscriptionDetailsByUserId(@PathVariable long userId){
        return ResponseEntity.ok(subscriptionService.getSubscriptionDetailsByUserId(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<SubscriptionDto> updateSubscriptionByUserId(@PathVariable long userId,@RequestBody SubscriptionDto subscriptionDto){
        return ResponseEntity.ok(subscriptionMealOffCoordinatorService.updateSubscriptionByUserId(userId,subscriptionDto));
    }

}
