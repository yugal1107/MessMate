package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.entities.EmailVerificationToken;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.repositories.EmailVerificationTokenRepository;
import com.springboot.MessApplication.MessMate.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("Should resend a verification link for an unverified user")
    void shouldResendVerificationLinkForUnverifiedUser() {
        User user = User.builder()
                .email("user@test.com")
                .name("User")
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.empty());

        emailVerificationService.resendVerificationEmail(user.getEmail());

        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendHtmlMail(
                org.mockito.ArgumentMatchers.eq(user.getEmail()),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    @DisplayName("Should not reveal whether an email can be sent")
    void shouldNotSendForUnknownEmail() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        emailVerificationService.resendVerificationEmail("unknown@test.com");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    @DisplayName("Should enforce the resend cooldown")
    void shouldEnforceResendCooldown() {
        User user = User.builder()
                .email("user@test.com")
                .emailVerified(false)
                .build();
        EmailVerificationToken existingToken = EmailVerificationToken.builder()
                .user(user)
                .createdAt(LocalDateTime.now().minusSeconds(30))
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findByUser(user)).thenReturn(Optional.of(existingToken));

        emailVerificationService.resendVerificationEmail(user.getEmail());

        verify(tokenRepository).findByUser(user);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should verify an unexpired link and activate the user")
    void shouldVerifyUnexpiredLinkAndActivateUser() throws Exception {
        String rawToken = "verification-token";
        User user = User.builder().email("user@test.com").emailVerified(false).build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .tokenHash(sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .user(user)
                .build();

        when(tokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));

        emailVerificationService.verifyEmail(rawToken);

        assertTrue(user.isEnabled());
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
