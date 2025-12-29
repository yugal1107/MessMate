package com.springboot.MessApplication.MessMate.dto;

import com.springboot.MessApplication.MessMate.entities.enums.Meal;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomMealOffDto {

    private Boolean customOff;

    private Meal startMeal;
    private Meal endMeal;

    private LocalDate startDate;
    private LocalDate endDate;
}
