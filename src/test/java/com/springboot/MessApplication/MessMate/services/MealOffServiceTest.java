package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.CustomMealOffDto;
import com.springboot.MessApplication.MessMate.dto.TodayMealOffDto;
import com.springboot.MessApplication.MessMate.entities.MealOff;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.Meal;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.Role;
import com.springboot.MessApplication.MessMate.repositories.MealOffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealOffServiceTest {

    @Mock
    private MealOffRepository mealOffRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MealOffService mealOffService;

    private User student;
    private MealOff mealOff;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(10L)
                .name("Alice")
                .role(Role.STUDENT)
                .build();

        mealOff = new MealOff();
        mealOff.setId(100L);
        mealOff.setUser(student);
    }

    @Test
    @DisplayName("Should cancel lunch off and notify student and all admins with student name and ID")
    void shouldCancelLunchOffAndNotifyStudentAndAllAdmins() {
        mealOff.setLunch(true);

        when(mealOffRepository.findByUser_Id(10L)).thenReturn(Optional.of(mealOff));
        when(mealOffRepository.save(any(MealOff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(MealOff.class), eq(TodayMealOffDto.class))).thenReturn(new TodayMealOffDto());

        TodayMealOffDto result = mealOffService.cancelLunchOffByUserId(10L);

        assertFalse(mealOff.getLunch());
        verify(subscriptionService).checkSubscriptionStatus(10L);
        verify(mealOffRepository).save(mealOff);

        // Verify user notification
        verify(notificationService).createNotification(
                10L,
                NotificationType.MEAL_OFF,
                "Your lunch off has been cancelled via admin"
        );

        // Verify admin broadcast notification
        verify(notificationService).notifyAllAdmins(
                NotificationType.MEAL_OFF,
                "Lunch off for Alice (ID: 10) was cancelled by admin"
        );
    }

    @Test
    @DisplayName("Should cancel dinner off and notify student and all admins with student name and ID")
    void shouldCancelDinnerOffAndNotifyStudentAndAllAdmins() {
        mealOff.setDinner(true);

        when(mealOffRepository.findByUser_Id(10L)).thenReturn(Optional.of(mealOff));
        when(mealOffRepository.save(any(MealOff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(MealOff.class), eq(TodayMealOffDto.class))).thenReturn(new TodayMealOffDto());

        TodayMealOffDto result = mealOffService.cancelDinnerOffByUserId(10L);

        assertFalse(mealOff.getDinner());
        verify(subscriptionService).checkSubscriptionStatus(10L);
        verify(mealOffRepository).save(mealOff);

        // Verify user notification
        verify(notificationService).createNotification(
                10L,
                NotificationType.MEAL_OFF,
                "Your Dinner off has been cancelled via admin"
        );

        // Verify admin broadcast notification
        verify(notificationService).notifyAllAdmins(
                NotificationType.MEAL_OFF,
                "Dinner off for Alice (ID: 10) was cancelled by admin"
        );
    }

    @Test
    @DisplayName("Should cancel custom meal off and notify student and all admins with student name and ID")
    void shouldCancelCustomOffAndNotifyStudentAndAllAdmins() {
        mealOff.setCustomOff(true);
        mealOff.setStartDate(LocalDate.now());
        mealOff.setEndDate(LocalDate.now().plusDays(2));
        mealOff.setStartMeal(Meal.LUNCH);
        mealOff.setEndMeal(Meal.DINNER);

        when(mealOffRepository.findByUser_Id(10L)).thenReturn(Optional.of(mealOff));
        when(mealOffRepository.save(any(MealOff.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(MealOff.class), eq(CustomMealOffDto.class))).thenReturn(new CustomMealOffDto());

        CustomMealOffDto result = mealOffService.cancelCustomOffByUserId(10L);

        assertFalse(mealOff.getCustomOff());
        assertNull(mealOff.getStartDate());
        assertNull(mealOff.getEndDate());
        assertNull(mealOff.getStartMeal());
        assertNull(mealOff.getEndMeal());

        verify(subscriptionService).checkSubscriptionStatus(10L);
        verify(mealOffRepository).save(mealOff);

        // Verify user notification
        verify(notificationService).createNotification(
                10L,
                NotificationType.MEAL_OFF,
                "your custom meal off has been cancelled by Admin"
        );

        // Verify admin broadcast notification
        verify(notificationService).notifyAllAdmins(
                NotificationType.MEAL_OFF,
                "Custom meal off for Alice (ID: 10) was cancelled by admin"
        );
    }
}
