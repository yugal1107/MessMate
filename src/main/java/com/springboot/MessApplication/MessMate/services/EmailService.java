package com.springboot.MessApplication.MessMate.services;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.springboot.MessApplication.MessMate.exceptions.EmailSendingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final Resend resend;

    @Value("${resend.from}")
    private String from;

    public void sendMail(String to, String subject, String body) {
        send(to, subject, null, body);
    }

    public void sendHtmlMail(String to, String subject, String htmlBody, String textBody) {
        send(to, subject, htmlBody, textBody);
    }

    private void send(String to, String subject, String htmlBody, String textBody) {
        CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .text(textBody);

        if (htmlBody != null && !htmlBody.isBlank()) {
            builder.html(htmlBody);
        }

        try {
            resend.emails().send(builder.build());
        } catch (Exception exception) {
            throw new EmailSendingException("Failed to send email", exception);
        }
    }
}
