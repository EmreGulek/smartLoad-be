package com.smartload.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Doğrulama e-postası: önce Resend HTTP API (Render free SMTP engeli), sonra Gmail SMTP, yoksa log.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String resendApiKey;
    private final String mailFrom;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmailService(
        @Autowired(required = false) JavaMailSender mailSender,
        @Value("${spring.mail.username:}") String mailUsername,
        @Value("${smartload.mail.resend-api-key:}") String resendApiKey,
        @Value("${smartload.mail.from:SmartLoad <onboarding@resend.dev>}") String mailFrom,
        ObjectMapper objectMapper
    ) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername != null ? mailUsername : "";
        this.resendApiKey = resendApiKey != null ? resendApiKey.trim() : "";
        this.mailFrom = mailFrom != null ? mailFrom.trim() : "SmartLoad <onboarding@resend.dev>";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public void sendVerificationCode(String toEmail, String code) {
        String body = buildBody(code);

        if (!resendApiKey.isBlank() && sendViaResend(toEmail, body)) {
            return;
        }

        if (mailSender != null && !mailUsername.isBlank() && sendViaSmtp(toEmail, body)) {
            return;
        }

        log.info(
            "[smartload] Mail not sent (configure RESEND_API_KEY on Render); verification code for {}: {}",
            toEmail,
            code
        );
    }

    private String buildBody(String code) {
        return "Merhaba,\n\n"
            + "SmartLoad hesabınızı doğrulamak için kodunuz: "
            + code
            + "\n\n"
            + "Bu kod 15 dakika geçerlidir.\n\n"
            + "Bu e-postayı siz istemediyseniz yok sayabilirsiniz.\n";
    }

    private boolean sendViaResend(String toEmail, String textBody) {
        try {
            Map<String, Object> payload = Map.of(
                "from", mailFrom,
                "to", List.of(toEmail),
                "subject", "SmartLoad — e-posta doğrulama",
                "text", textBody
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Resend: verification email sent to {}", toEmail);
                return true;
            }
            log.warn("Resend API {} for {}: {}", response.statusCode(), toEmail, response.body());
        } catch (Exception e) {
            log.warn("Resend failed for {}: {}", toEmail, e.getMessage());
        }
        return false;
    }

    private boolean sendViaSmtp(String toEmail, String textBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(toEmail);
            message.setSubject("SmartLoad — e-posta doğrulama");
            message.setText(textBody);
            mailSender.send(message);
            log.info("SMTP: verification email sent to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("SMTP failed for {} (Render free blocks port 587): {}", toEmail, e.getMessage());
            return false;
        }
    }
}
