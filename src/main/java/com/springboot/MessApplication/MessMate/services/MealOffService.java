package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.*;
import com.springboot.MessApplication.MessMate.entities.MealOff;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.Meal;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.exceptions.MealOffDeadlineException;
import com.springboot.MessApplication.MessMate.exceptions.ResourceNotFoundException;
import com.springboot.MessApplication.MessMate.repositories.MealOffRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealOffService {

    private final LocalTime LUNCH_DEADLINE = LocalTime.of(8, 0,0);
    private final LocalTime DINNER_DEADLINE = LocalTime.of(16, 0,0);
    private final MealOffRepository mealOffRepository;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final UserService userService;

    public TodayMealOffDto setLunchOff() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());
        MealOff mealOff = getMealOff(user.getId());
        if(mealOff.getLunch()){
            TodayMealOffDto todayMealOffDto = modelMapper.map(mealOff, TodayMealOffDto.class);
            todayMealOffDto.setMessage("Lunch Already set off for today");
            return todayMealOffDto;
        }else{
            if(LocalTime.now().isBefore(LUNCH_DEADLINE)) {
                mealOff.setLunch(true);
                MealOff savedMealOff = mealOffRepository.save(mealOff);
                TodayMealOffDto todayMealOffDto = modelMapper.map(savedMealOff, TodayMealOffDto.class);
                todayMealOffDto.setMessage("Lunch set off for today successfully");
                return todayMealOffDto;
            }else {
                throw new MealOffDeadlineException("Cannot set lunch off after " + LUNCH_DEADLINE);
            }
        }
    }
    public TodayMealOffDto cancelLunchOff() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());
        MealOff mealOff = getMealOff(user.getId());
        if(!mealOff.getLunch()){
            TodayMealOffDto todayMealOffDto = modelMapper.map(mealOff, TodayMealOffDto.class);
            todayMealOffDto.setMessage("Lunch not set off for today");
            return todayMealOffDto;
        }else if(LocalTime.now().isBefore(LUNCH_DEADLINE)) {
            mealOff.setLunch(false);
            MealOff savedMealOff = mealOffRepository.save(mealOff);
            TodayMealOffDto todayMealOffDto = modelMapper.map(savedMealOff, TodayMealOffDto.class);
            todayMealOffDto.setMessage("Lunch off cancelled successfully");
            return todayMealOffDto;
        }else{
            throw new MealOffDeadlineException("Cannot cancel Lunch off after " + LUNCH_DEADLINE);
        }
    }

    public TodayMealOffDto setDinnerOff() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());
        MealOff mealOff = getMealOff(user.getId());
        if(mealOff.getDinner()) {
            TodayMealOffDto todayMealOffDto = modelMapper.map(mealOff, TodayMealOffDto.class);
            todayMealOffDto.setMessage("Dinner Already set off for today");
            return todayMealOffDto;
        }else{
            if(LocalTime.now().isBefore(DINNER_DEADLINE)) {
                mealOff.setDinner(true);
                MealOff savedMealoff = mealOffRepository.save(mealOff);
                TodayMealOffDto todayMealOffDto = modelMapper.map(savedMealoff, TodayMealOffDto.class);
                todayMealOffDto.setMessage("Dinner set off for today successfully");
                return todayMealOffDto;
            }else {
                throw new MealOffDeadlineException("Cannot set dinner off after " + DINNER_DEADLINE);
            }
        }
    }

    public TodayMealOffDto cancelDinnerOff() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());
        MealOff mealOff = getMealOff(user.getId());
        if(!mealOff.getDinner()){
            TodayMealOffDto todayMealOffDto = modelMapper.map(mealOff, TodayMealOffDto.class);
            todayMealOffDto.setMessage("Dinner not set off for today");
            return todayMealOffDto;
        }else if(LocalTime.now().isBefore(DINNER_DEADLINE)) {
            mealOff.setDinner(false);
            MealOff savedMealOff = mealOffRepository.save(mealOff);
            TodayMealOffDto todayMealOffDto = modelMapper.map(savedMealOff, TodayMealOffDto.class);
            todayMealOffDto.setMessage("Dinner off cancelled successfully");
            return todayMealOffDto;
        }else{
            throw new MealOffDeadlineException("Cannot cancel Dinner off after " + DINNER_DEADLINE);
        }
    }

    public CustomMealOffDto setCustomMealOff(CustomMealOffDto mealOffDto) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());

        MealOff mealOff = getMealOff(user.getId());
        if(mealOff.getCustomOff()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Custom off already set");
        }

        if(mealOffDto.getStartDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Start date cannot be before current date");
        }

        if(mealOffDto.getEndDate().isBefore(mealOffDto.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"End date cannot be before Start date");
        }

        if(mealOffDto.getStartDate().isEqual(mealOffDto.getEndDate()) && mealOffDto.getStartMeal()==Meal.DINNER && mealOffDto.getEndMeal()==Meal.LUNCH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Start meal cannot be after end meal for one day off");
        }

        boolean isFutureDate = mealOffDto.getStartDate().isAfter(LocalDate.now());
        boolean lunchAllowed = mealOffDto.getStartMeal() == Meal.LUNCH && LocalTime.now().isBefore(LUNCH_DEADLINE);
        boolean dinnerAllowed = mealOffDto.getStartMeal() == Meal.DINNER && LocalTime.now().isBefore(DINNER_DEADLINE);

        if( isFutureDate || lunchAllowed || dinnerAllowed){
            modelMapper.map(mealOffDto, mealOff);
            mealOff.setCustomOff(true);
            MealOff savedMealOff = mealOffRepository.save(mealOff);
            //creating notification
            String message = "Meals set off successfully from " +savedMealOff.getStartDate() + " " + savedMealOff.getStartMeal() + " to " +savedMealOff.getEndDate() + " " +  savedMealOff.getEndMeal();
            notificationService.createNotification(user.getId(), NotificationType.MEAL_UPDATE,message);
            return modelMapper.map(savedMealOff, CustomMealOffDto.class);
        }else {
            throw new MealOffDeadlineException("Today's Deadline for "+ mealOffDto.getStartMeal() + " is missed. Please try again");
        }
    }

    public CustomMealOffDto cancelCustomMealOff() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());
        MealOff mealOff = getMealOff(user.getId());
        //check if meal off exists or not
        if (!mealOff.getCustomOff() || mealOff.getStartDate() == null || mealOff.getEndDate() == null ||
                mealOff.getStartMeal() == null || mealOff.getEndMeal() == null){
            throw new ResourceNotFoundException("No existing meal off found");
        }else{
            mealOff.setCustomOff(false);
            mealOff.setStartMeal(null);
            mealOff.setEndMeal(null);
            mealOff.setStartDate(null);
            mealOff.setEndDate(null);

            //save mealOff
            MealOff savedMealOff = mealOffRepository.save(mealOff);

            //creating notification
            notificationService.createNotification(user.getId(), NotificationType.MEAL_UPDATE, "your custom meal off has been cancelled");
            return modelMapper.map(savedMealOff, CustomMealOffDto.class);
        }
    }


    public TodayMealOffDto getTodayMealOffDetails() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());
        MealOff mealOff = getMealOff(user.getId());
        return modelMapper.map(mealOff, TodayMealOffDto.class);
    }

    public CustomMealOffDto getCustomMealOffDetails() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        subscriptionService.checkSubscriptionStatus(user.getId());
        MealOff mealOff = getMealOff(user.getId());
        return modelMapper.map(mealOff, CustomMealOffDto.class);
    }

    public UserListDto getAllLunchOffs() {
        List<User> lunchOffUsers = userService.getLunchOffUsers();
        List<UserDto> lunchOffUserDtoList = lunchOffUsers
                .stream()
                .map(
                user -> modelMapper.map(user,UserDto.class)
                )
                .toList();
        return new UserListDto(lunchOffUserDtoList.size(), lunchOffUserDtoList);
    }

    public UserListDto getAllDinnerOffs() {
        List<User> dinnerOffUsers = userService.getDinnerOffUsers();
        List<UserDto> dinnerOffUserDtoList = dinnerOffUsers
                .stream()
                .map(
                        user -> modelMapper.map(user,UserDto.class)
                )
                .toList();
        return new UserListDto(dinnerOffUserDtoList.size(), dinnerOffUserDtoList);
    }

    public List<CustomOffDetailDto> getAllCustomOffs() {
        return mealOffRepository.findAllByCustomOff(true)
                .stream()
                .map(mealOff -> {
                    CustomOffDetailDto customOffDetail = new CustomOffDetailDto();
                    customOffDetail.setCustomMealOff(modelMapper.map(mealOff, CustomMealOffDto.class));
                    //since fetch type is eager for User in MealOff, we can directly get user from mealOff object
                    //we do not need to make the database call
                    customOffDetail.setUser(modelMapper.map(mealOff.getUser(), UserDto.class));
                    return customOffDetail;
                })
                .toList();
    }

    public CustomMealOffDto getCustomOffDetailsByUserId(Long userId) {
        subscriptionService.checkSubscriptionStatus(userId);
        MealOff mealOff = getMealOff(userId);
        return modelMapper.map(mealOff, CustomMealOffDto.class);
    }

    public CustomMealOffDto cancelCustomOffByUserId(Long userId) {
        subscriptionService.checkSubscriptionStatus(userId);
        MealOff mealOff = getMealOff(userId);
        //check if meal off exists or not
        if (!mealOff.getCustomOff() || mealOff.getStartDate() == null || mealOff.getEndDate() == null ||
                mealOff.getStartMeal() == null || mealOff.getEndMeal() == null){
            throw new ResourceNotFoundException("No existing meal off found");
        }else {
            mealOff.setCustomOff(false);
            mealOff.setStartMeal(null);
            mealOff.setEndMeal(null);
            mealOff.setStartDate(null);
            mealOff.setEndDate(null);

            //save mealOff
            MealOff savedMealOff = mealOffRepository.save(mealOff);

            //creating notification
            notificationService.createNotification(userId, NotificationType.MEAL_UPDATE, "your custom meal off has been cancelled by Admin");
            return modelMapper.map(savedMealOff, CustomMealOffDto.class);
        }
    }

    // non-controller methods
    public MealOff getMealOff(long userId) {
        return mealOffRepository.findByUser_Id(userId);
    }

    public void saveMealOff(MealOff mealOff) {
        mealOffRepository.save(mealOff);
    }



    public List<MealOff>  getCustomMealOffs() {
        return mealOffRepository.findAllByCustomOff(true);
    }

    public void resetMealOff(Long userId) {
        MealOff mealOff = getMealOff(userId);
        mealOff.setLunch(false);
        mealOff.setDinner(false);
        mealOff.setCustomOff(false);
        mealOff.setStartMeal(null);
        mealOff.setEndMeal(null);
        mealOff.setStartDate(null);
        mealOff.setEndDate(null);
        mealOffRepository.save(mealOff);
    }


    public void resetLunchOffs(){
        List<MealOff> lunchOffs = mealOffRepository.findAllByLunchAndUser_Subscription_Status(true,SubscriptionStatus.ACTIVE);
        for(MealOff mealOff : lunchOffs){
            User user = mealOff.getUser();

            //creating notification
            notificationService.createNotification(
                    user.getId(), NotificationType.MEAL_UPDATE, "Lunch set off successfully for " + LocalDate.now()
            );
            mealOff.setLunch(false);
        }
        mealOffRepository.saveAll(lunchOffs);
    }

    public void resetDinnerOffs(){
        List<MealOff> dinnerOffs = mealOffRepository.findAllByDinnerAndUser_Subscription_Status(true,SubscriptionStatus.ACTIVE);
        for(MealOff mealOff : dinnerOffs){
            User user = mealOff.getUser();

            //creating notification
            notificationService.createNotification(
                    user.getId(), NotificationType.MEAL_UPDATE, "Dinner set off successfully for " + LocalDate.now()
            );
            mealOff.setDinner(false);
        }
        mealOffRepository.saveAll(dinnerOffs);
    }

}
