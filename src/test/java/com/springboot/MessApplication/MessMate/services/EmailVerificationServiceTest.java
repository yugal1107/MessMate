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
