package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.ChangePasswordRequestDto;
import com.springboot.MessApplication.MessMate.dto.SignupDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.dto.UpdateProfileDto;
import com.springboot.MessApplication.MessMate.dto.UserDto;
import com.springboot.MessApplication.MessMate.dto.UserListDto;
import com.springboot.MessApplication.MessMate.entities.MealOff;
import com.springboot.MessApplication.MessMate.entities.Subscription;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.NotificationType;
import com.springboot.MessApplication.MessMate.entities.enums.Role;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionType;
import com.springboot.MessApplication.MessMate.exceptions.BadRequestException;
import com.springboot.MessApplication.MessMate.exceptions.ResourceNotFoundException;
import com.springboot.MessApplication.MessMate.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public UserService(
            UserRepository userRepository,
            ModelMapper modelMapper,
            PasswordEncoder passwordEncoder,
            @Lazy NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserDto signup(SignupDto signupDto) {
        Optional<User> user = userRepository.findByEmail(signupDto.getEmail());
        if(user.isPresent()){
            throw new BadCredentialsException("User with email " +  signupDto.getEmail() + " already exists");
        }

        User toBeCreatedUser = modelMapper.map(signupDto, User.class);
        toBeCreatedUser.setRole(Role.STUDENT);

        Subscription subscription = Subscription.builder().status(SubscriptionStatus.INACTIVE).build();
        toBeCreatedUser.setSubscription(subscription);

        MealOff mealoff = new  MealOff();
        toBeCreatedUser.setMealOff(mealoff);

        toBeCreatedUser.setPassword(passwordEncoder.encode(signupDto.getPassword()));
        return modelMapper.map(userRepository.save(toBeCreatedUser), UserDto.class);
    }

    public UserListDto getAllUsersFilteredBySubscriptionStatusAndType(SubscriptionStatus status, SubscriptionType type) {
        List<User> users;
        if(status!=null){
            if(status == SubscriptionStatus.ACTIVE && type!=null){
                users = userRepository.findBySubscription_statusAndSubscription_type(status,type);
            }else{
                users = userRepository.findBySubscription_Status(status);
            }

        }else{
            users = userRepository.findAll();
        }
        List<UserDto> usersDtos = users.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
        return new UserListDto(usersDtos.size(),usersDtos);
    }

    public UserDto getMyProfile() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return modelMapper.map(user, UserDto.class);
    }

    @Transactional
    public UserDto updateMyProfile(UpdateProfileDto dto) {
        if (dto == null) {
            throw new BadRequestException("Profile update payload cannot be null");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("Name cannot be empty");
        }
        User authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = getUserById(authUser.getId());

        user.setName(dto.getName().trim());
        if (dto.getContact() != null) {
            user.setContact(dto.getContact().trim());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress().trim());
        }

        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    public UserDto getUserProfileById(long id) {
        User user = getUserById(id);
        return modelMapper.map(user, UserDto.class);
    }

    @Transactional
    public UserDto updateUserProfileById(Long id, UpdateProfileDto dto) {
        if (dto == null) {
            throw new BadRequestException("Profile update payload cannot be null");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("Name cannot be empty");
        }
        User user = getUserById(id);

        user.setName(dto.getName().trim());
        if (dto.getContact() != null) {
            user.setContact(dto.getContact().trim());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress().trim());
        }

        User savedUser = userRepository.save(user);

        notificationService.createNotification(
                id,
                NotificationType.ADMIN_UPDATE,
                "Your profile details have been updated by admin"
        );

        return modelMapper.map(savedUser, UserDto.class);
    }

    public UserListDto searchUsersByName(String name) {
        List<UserDto> userDtoList = userRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
        return new UserListDto(userDtoList.size(), userDtoList);
    }

    @Transactional
    public SuccessResponseDto changePassword(ChangePasswordRequestDto dto) {
        if (dto == null) {
            throw new BadRequestException("Password change payload cannot be null");
        }
        if (dto.getCurrentPassword() == null || dto.getCurrentPassword().trim().isEmpty()) {
            throw new BadRequestException("Current password cannot be empty");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().trim().isEmpty()) {
            throw new BadRequestException("New password cannot be empty");
        }
        if (dto.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long");
        }

        User authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = getUserById(authUser.getId());

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        return new SuccessResponseDto("Password changed successfully");
    }

    //non controller methods
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public List<User> getSubscribedUsers(){
        return userRepository.findBySubscription_Status(SubscriptionStatus.ACTIVE);
    }

    public User findByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(()->new ResourceNotFoundException("user not found"));
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    public List<User> getLunchOffUsers(){
        return userRepository.findByMealOff_Lunch(true);
    }

    public List<User> getDinnerOffUsers() {
        return userRepository.findByMealOff_Dinner(true);
    }

    public List<User> getStudents() {
        return userRepository.findByRole(Role.STUDENT);
    }

    public List<User> getActiveSubscribedStudents() {
        return userRepository.findByRoleAndSubscription_Status(Role.STUDENT, SubscriptionStatus.ACTIVE);
    }

}
