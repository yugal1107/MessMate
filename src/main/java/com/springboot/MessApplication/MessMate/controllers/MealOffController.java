package com.springboot.MessApplication.MessMate.controllers;

import com.springboot.MessApplication.MessMate.dto.*;
import com.springboot.MessApplication.MessMate.services.MealOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/mealoff")
@RequiredArgsConstructor
public class MealOffController {

    private final MealOffService mealOffService;

    @PostMapping("/lunch")
    public ResponseEntity<TodayMealOffDto> setLunchOff() {
        return ResponseEntity.ok(mealOffService.setLunchOff());
    }

    @DeleteMapping("/lunch")
    public  ResponseEntity<TodayMealOffDto> cancelLunchOff() {
        return ResponseEntity.ok(mealOffService.cancelLunchOff());
    }

    @PostMapping("/dinner")
    public ResponseEntity<TodayMealOffDto> setDinnerOff() {
        return ResponseEntity.ok(mealOffService.setDinnerOff());
    }

    @DeleteMapping("/dinner")
    public  ResponseEntity<TodayMealOffDto> cancelDinnerOff() {
        return ResponseEntity.ok(mealOffService.cancelDinnerOff());
    }

    @PostMapping
    public ResponseEntity<CustomMealOffDto> setMealOff(@RequestBody CustomMealOffDto mealOffDto) {
        return ResponseEntity.ok(mealOffService.setCustomMealOff(mealOffDto));
    }

    @DeleteMapping
    public ResponseEntity<CustomMealOffDto> cancelMealOff(){
        return ResponseEntity.ok(mealOffService.cancelCustomMealOff());
    }

    //getMealOffDetails
    @GetMapping("/today")
    public ResponseEntity<TodayMealOffDto> getTodayMealOffDetails(){
        return ResponseEntity.ok(mealOffService.getTodayMealOffDetails());
    }

    //getCustomMealOffDetails
    @GetMapping("/custom")
    public ResponseEntity<CustomMealOffDto> getCustomMealOffDetails(){
        return ResponseEntity.ok(mealOffService.getCustomMealOffDetails());
    }

    //get all lunch off (for admin)
    @GetMapping("/lunch_offs")
    public  ResponseEntity<UserListDto> getAllLunchOffs(){
        return ResponseEntity.ok(mealOffService.getAllLunchOffs());
    }


    //get all dinner off (for admin)
    @GetMapping("/dinner_offs")
    public ResponseEntity<UserListDto> getAllDinnerOffs(){
        return ResponseEntity.ok(mealOffService.getAllDinnerOffs());
    }

    //get all custom meal off (for admin)
    @GetMapping("/custom_offs")
    public ResponseEntity<List<CustomOffDetailDto>> getAllCustomOffDetails(){
        return ResponseEntity.ok(mealOffService.getAllCustomOffs());
    }

    //get custom meal off details by userId (for admin)
    @GetMapping("/custom/{userId}")
    public ResponseEntity<CustomMealOffDto> getCustomOffDetailsByUserId(@PathVariable Long userId){
        return ResponseEntity.ok(mealOffService.getCustomOffDetailsByUserId(userId));
    }

    //cancel custom off by userId (for admin)
    @DeleteMapping("/custom/{userId}")
    public ResponseEntity<CustomMealOffDto> cancelCustomOffByUserId(@PathVariable Long userId){
        return ResponseEntity.ok(mealOffService.cancelCustomOffByUserId(userId));
    }

}
