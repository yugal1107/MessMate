package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.dto.SignupDto;
import com.springboot.MessApplication.MessMate.dto.UserDto;
import com.springboot.MessApplication.MessMate.dto.UserListDto;
import com.springboot.MessApplication.MessMate.entities.MealOff;
import com.springboot.MessApplication.MessMate.entities.Subscription;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.entities.enums.Role;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.exceptions.ResourceNotFoundException;
import com.springboot.MessApplication.MessMate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

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

    public UserListDto getAllUsersFilteredBySubscriptionStatus(SubscriptionStatus status) {
        List<User> users;
        if(status!=null){
            users = userRepository.findBySubscription_Status(status);
        }else{
            users = userRepository.findAll();
        }
        List<UserDto> usersDtos = users.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .collect(Collectors.toList());
        return new UserListDto(usersDtos.size(),usersDtos);
    }

    public UserDto getMyProfile() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return modelMapper.map(user, UserDto.class);
    }

    public UserDto getUserProfileById(long id) {
        User user = getUserById(id);
        return modelMapper.map(user, UserDto.class);
    }

    public UserListDto searchUsersByName(String name) {
        List<UserDto> userDtoList = userRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
        return new UserListDto(userDtoList.size(), userDtoList);
    }

    //non controller methods
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public List<User> getSubscribedUsers(){
        return userRepository.findBySubscription_Status(SubscriptionStatus.ACTIVE);
    }

    public List<User> getLunchOffUsers(){
        return userRepository.findByMealOff_Lunch(true);
    }

    public List<User> getDinnerOffUsers() {
        return userRepository.findByMealOff_Dinner(true);
    }

}
