package com.springboot.MessApplication.MessMate.controllers;

import com.springboot.MessApplication.MessMate.dto.ChangePasswordRequestDto;
import com.springboot.MessApplication.MessMate.dto.SuccessResponseDto;
import com.springboot.MessApplication.MessMate.dto.UpdateProfileDto;
import com.springboot.MessApplication.MessMate.dto.UserDto;
import com.springboot.MessApplication.MessMate.dto.UserListDto;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionStatus;
import com.springboot.MessApplication.MessMate.entities.enums.SubscriptionType;
import com.springboot.MessApplication.MessMate.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserDto> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping
    public ResponseEntity<UserDto> updateMyProfile(@RequestBody UpdateProfileDto dto) {
        return ResponseEntity.ok(userService.updateMyProfile(dto));
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUserProfileById(
            @PathVariable Long id,
            @RequestBody UpdateProfileDto dto
    ) {
        return ResponseEntity.ok(userService.updateUserProfileById(id, dto));
    }

    @PutMapping("/change-password")
    public ResponseEntity<SuccessResponseDto> changePassword(@RequestBody ChangePasswordRequestDto dto) {
        return ResponseEntity.ok(userService.changePassword(dto));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/all")
    public ResponseEntity<UserListDto> getAllUsers(
            @RequestParam(value = "status" , required = false) SubscriptionStatus status,
            @RequestParam(value = "type", required = false) SubscriptionType type
            ) {
        UserListDto userListDto = userService.getAllUsersFilteredBySubscriptionStatusAndType(status,type);
        return ResponseEntity.ok(userListDto);
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserProfileById(@PathVariable long id) {
        return ResponseEntity.ok(userService.getUserProfileById(id));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/search/{name}")
    public ResponseEntity<UserListDto> searchUsersByName(@PathVariable String name ) {
        return ResponseEntity.ok(userService.searchUsersByName(name));
    }

}
