package com.jobportal.backend.service.impl;

import com.jobportal.backend.entity.User;
import com.jobportal.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.support.email:support@jobportal.com}")
    private String supportEmail;

    @Value("${spring.mail.username}") // Get email from application.yml
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendPasswordResetEmail(User user, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("🔐 Password Reset Request - JobPortal");
            helper.setFrom(fromEmail, "JobPortal Security"); // Use configured email

            // Create Thymeleaf context with all required variables
            Context context = new Context();
            context.setVariable("name", user.getUsername());
            context.setVariable("token", formatOTP(token));
            context.setVariable("expirationHours", 1);
            context.setVariable("resetLink", frontendUrl + "/reset-password?token=" + token);
            context.setVariable("currentYear", LocalDateTime.now().getYear());
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("requestTime", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

            String htmlContent = templateEngine.process("password-reset-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            logger.info("Password reset email sent successfully to: {}", user.getEmail());

        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        } catch (Exception e) {
            logger.error("Error processing email template for: {}", user.getEmail(), e);
            throw new RuntimeException("Error processing email template", e);
        }
    }

    private String formatOTP(String token) {
        // Now token is already a 6-digit OTP, just format it with spaces
        if (token.length() == 6) {
            return String.join(" ", token.split(""));
        }
        return token;
    }

    /**
     * Send welcome email after registration
     */
    public void sendWelcomeEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("🎉 Welcome to NepTalent!");
            helper.setFrom(fromEmail, "NepTalent Team");

            Context context = new Context();
            context.setVariable("name", user.getUsername());
            context.setVariable("currentYear", LocalDateTime.now().getYear());
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("loginLink", frontendUrl + "/login");

            String htmlContent = templateEngine.process("welcome-email", context);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            logger.info("Welcome email sent successfully to: {}", user.getEmail());

        } catch (Exception e) {
            // Log error but don't throw - welcome email failure shouldn't block registration
            logger.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }
}