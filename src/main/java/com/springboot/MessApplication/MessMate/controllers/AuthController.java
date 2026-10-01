package com.springboot.MessApplication.MessMate.controllers;


import com.springboot.MessApplication.MessMate.dto.*;
import com.springboot.MessApplication.MessMate.services.AuthService;
import com.springboot.MessApplication.MessMate.services.EmailVerificationService;
import com.springboot.MessApplication.MessMate.services.PasswordResetTokenService;
import com.springboot.MessApplication.MessMate.services.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailVerificationService emailVerificationService;

    @Value("${deploy.env}")
    private String deployEnv;

    
    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@Valid @RequestBody SignupDto signupDto) {
        UserDto userDto = userService.signup(signupDto);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<SuccessResponseDto> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(new SuccessResponseDto("Email verified successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto, HttpServletResponse response) {
        LoginResponseDto loginResponseDto = authService.login(loginDto);

        Cookie cookie = new Cookie("refreshToken", loginResponseDto.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure("production".equals(deployEnv));
        response.addCookie(cookie);

        return ResponseEntity.ok(loginResponseDto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestBody RefreshTokenDto refreshTokenDto) {
//        String refreshToken = Arrays.stream(request.getCookies())
//                .filter(c -> c.getName().equals("refreshToken"))
//                .findFirst()
//                .map(Cookie::getValue)
//                .orElseThrow(() -> new AuthenticationServiceException("refreshToken not found in the cookies"));

        LoginResponseDto loginResponseDto = authService.refreshToken(refreshTokenDto.getRefreshToken());
        return ResponseEntity.ok(loginResponseDto);
    }

    //forgot password
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponseDto> forgotPassword(
            @RequestBody ForgotPasswordRequestDto requestDto
    ){
        String resetToken = authService.forgotPassword(requestDto.getEmail());
        return ResponseEntity.ok(new ForgotPasswordResponseDto(resetToken));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody VerifyOtpRequestDto request
    ){
        passwordResetTokenService.verifyOtp(request.getResetToken(),request.getOtp());
        return ResponseEntity.ok(new SuccessResponseDto("Otp verified successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequestDto request
    ){
        passwordResetTokenService.resetPassword(request.getResetToken(),request.getNewPassword());
        return ResponseEntity.ok(new SuccessResponseDto("Password reset successfully"));
    }

}
