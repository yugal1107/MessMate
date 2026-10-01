package com.springboot.MessApplication.MessMate.services;

import com.springboot.MessApplication.MessMate.entities.PasswordResetToken;
import com.springboot.MessApplication.MessMate.entities.User;
import com.springboot.MessApplication.MessMate.exceptions.PasswordResetException;
import com.springboot.MessApplication.MessMate.repositories.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserService userService;

    @Transactional
    public String createResetTokenAndSendOtp(User user) {
        //delete an existing token
        passwordResetTokenRepository.deleteByUser(user);

        String resetToken = UUID.randomUUID().toString();
        String otp = generateOtp();

        //create token
        PasswordResetToken token = PasswordResetToken.builder()
                .resetToken(resetToken)
                .hashedOtp(passwordEncoder.encode(otp))
                .attemptCount(0)
                .otpVerified(false)
                .otpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .tokenExpiry(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .user(user)
                .build();

        //save token
        passwordResetTokenRepository.save(token);

        //send email to user
        emailService.sendHtmlMail(
                user.getEmail(),
                "Password Reset OTP - MessMate",
                buildPasswordResetHtmlEmail(user.getName(), otp),
                "Hi " + user.getName() + ", your MessMate password reset OTP is " + otp
                        + ". It is valid for 5 minutes."
        );

        //return token
        return token.getResetToken();
    }

    private String generateOtp() {
        return String.valueOf((int)(Math.random()*900000)+100000 );
    }

    private String buildPasswordResetHtmlEmail(String userName, String otp) {
        String safeUserName = escapeHtml(userName);
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
                                    <h2 style="margin:0 0 24px;color:#1a1a1a;">Reset your password</h2>
                                    <p style="color:#4a4a4a;line-height:1.6;">Hi %s,</p>
                                    <p style="color:#4a4a4a;line-height:1.6;">
                                        Use the OTP below to reset your MessMate password.
                                    </p>
                                    <p style="margin:28px 0;text-align:center;color:#1a1a1a;
                                              font-size:30px;font-weight:bold;letter-spacing:8px;">%s</p>
                                    <p style="color:#ad6800;background:#fffbe6;border:1px solid #ffe58f;
                                              padding:14px;border-radius:4px;">
                                        <strong>Valid for 5 minutes.</strong> Do not share this OTP.
                                    </p>
                                </td></tr>
                                <tr><td style="padding:24px 40px;background:#fafafa;color:#888;font-size:12px;">
                                    If you did not request this, you can safely ignore this email.
                                </td></tr>
                            </table>
                        </td></tr>
                    </table>
                </body>
                </html>
                """.formatted(safeUserName, otp);
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

    public void verifyOtp(String resetToken, String otp) {
        PasswordResetToken token = passwordResetTokenRepository.findByResetToken(resetToken)
                .orElseThrow(()->new PasswordResetException("Invalid session"));

        if(token.isOtpVerified()){
            throw new PasswordResetException("OTP already verified");
        }

        if(token.getTokenExpiry().isBefore(LocalDateTime.now())){
            throw new PasswordResetException("Reset session expired");
        }

        if(token.getOtpExpiry().isBefore(LocalDateTime.now())){
            throw new PasswordResetException("OTP expired");
        }

        if(token.getAttemptCount()>=MAX_ATTEMPTS){
            throw new PasswordResetException("Too many attempts");
        }

        if(!passwordEncoder.matches(otp,token.getHashedOtp())){
            token.setAttemptCount(token.getAttemptCount() + 1);
            passwordResetTokenRepository.save(token);
            throw new PasswordResetException("Invalid OTP");
        }

        token.setOtpVerified(true);
        passwordResetTokenRepository.save(token);
    }

    public void resetPassword(String resetToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByResetToken(resetToken)
                .orElseThrow(()->new PasswordResetException("Invalid Session"));

        if(!token.isOtpVerified()){
            throw new PasswordResetException("OTP not verified");
        }

        if(token.getTokenExpiry().isBefore(LocalDateTime.now())){
            throw new PasswordResetException("Reset session expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);

        passwordResetTokenRepository.delete(token);
    }
}
