package com.smartload.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Transactional e-posta gönderimi (doğrulama kodu vb.).
 * {@code JavaMailSender} yoksa (ör. test profili) veya {@code spring.mail.username} boşsa
 * kod yalnızca loglanır.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String mailUsername;

    public EmailService(
        @Autowired(required = false) JavaMailSender mailSender,
        @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername != null ? mailUsername : "";
    }

    public void sendVerificationCode(String toEmail, String code) {
        if (mailSender == null || mailUsername.isBlank()) {
            log.info(
                "[smartload] Mail not configured; verification code for {}: {}",
                toEmail,
                code
            );
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(toEmail);
            message.setSubject("SmartLoad — e-posta doğrulama");
            message.setText(
                "Merhaba,\n\n"
                    + "SmartLoad hesabınızı doğrulamak için kodunuz: "
                    + code
                    + "\n\n"
                    + "Bu kod 15 dakika geçerlidir.\n\n"
                    + "Bu e-postayı siz istemediyseniz yok sayabilirsiniz.\n"
            );
            mailSender.send(message);
            log.debug("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", toEmail, e);
            throw new RuntimeException("Could not send verification email. Please try again later.", e);
        }
    }
}
