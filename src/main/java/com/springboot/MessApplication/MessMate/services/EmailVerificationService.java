package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.entities.EmailVerificationToken;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.exceptions.BadRequestException;
import com.springboot.MessApplication.MessMate.repositories.EmailVerificationTokenRepository;
import com.springboot.MessApplication.MessMate.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int TOKEN_EXPIRY_HOURS = 24;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void createTokenAndSendVerificationLink(User user) {
        tokenRepository.deleteByUser(user);

        String rawToken = UUID.randomUUID().toString();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .user(user)
                .build();
        tokenRepository.save(token);

        String verificationLink = frontendUrl + "/verify-email?token=" + rawToken;
        emailService.sendHtmlMail(
                user.getEmail(),
                "Verify your MessMate email",
                buildVerificationEmail(user.getName(), verificationLink),
                "Verify your MessMate email by opening this link: " + verificationLink
        );
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid email verification link"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Email verification link has expired");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(token);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String buildVerificationEmail(String userName, String verificationLink) {
        String safeUserName = escapeHtml(userName);
        String safeLink = escapeHtml(verificationLink);
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" role="presentation">
                        <tr><td align="center" style="padding:40px 0;">
                            <table width="560" cellpadding="0" cellspacing="0" role="presentation"
                                   style="background:#ffffff;border-radius:8px;">
                                <tr><td style="padding:32px 40px;border-bottom:1px solid #e5e5e5;">
                                    <h1 style="margin:0;color:#1a1a1a;font-size:24px;">MessMate</h1>
                                </td></tr>
                                <tr><td style="padding:40px;">
                                    <h2 style="margin:0 0 24px;color:#1a1a1a;">Verify your email</h2>
                                    <p style="color:#4a4a4a;line-height:1.6;">Hi %s,</p>
                                    <p style="color:#4a4a4a;line-height:1.6;">
                                        Click the button below to verify your email address and activate your account.
                                    </p>
                                    <p style="text-align:center;margin:32px 0;">
                                        <a href="%s" style="background:#1677ff;color:#ffffff;padding:12px 24px;
                                           border-radius:4px;text-decoration:none;">Verify email</a>
                                    </p>
                                    <p style="color:#888;font-size:13px;line-height:1.5;">
                                        This link is valid for 24 hours and can only be used once.
                                    </p>
                                </td></tr>
                                <tr><td style="padding:24px 40px;background:#fafafa;color:#888;font-size:12px;">
                                    If you did not create a MessMate account, you can safely ignore this email.
                                </td></tr>
                            </table>
                        </td></tr>
                    </table>
                </body>
                </html>
                """.formatted(safeUserName, safeLink);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "there";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
