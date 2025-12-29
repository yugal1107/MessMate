package com.springboot.MessApplication.MessMate.controllers;

import com.springboot.MessApplication.MessMate.dto.UserDto;
import com.springboot.MessApplication.MessMate.dto.UserListDto;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/all")
    public ResponseEntity<UserListDto> getAllUsers(
            @RequestParam(value = "status" , required = false) SubscriptionStatus status
    ) {
        UserListDto userListDto = userService.getAllUsersFilteredBySubscriptionStatus(status);
        return ResponseEntity.ok(userListDto);
    }

    //TODO getUserDetailsById (admin)
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserProfileById(@PathVariable long id) {
        return ResponseEntity.ok(userService.getUserProfileById(id));
    }

    //TODO getMyProfile (student)
    @GetMapping
    public ResponseEntity<UserDto> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    //TODO updateProfile (all)

    @GetMapping("/search/{name}")
    public ResponseEntity<UserListDto> searchUsersByName(@PathVariable String name ) {
        return ResponseEntity.ok(userService.searchUsersByName(name));
    }

}
