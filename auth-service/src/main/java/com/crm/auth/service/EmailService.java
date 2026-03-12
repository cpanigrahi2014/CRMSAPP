package com.crm.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@crmsapp.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:CRM Platform}")
    private String fromName;

    @Value("${app.password-reset.base-url:http://localhost:3003}")
    private String frontendBaseUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String token, String firstName) {
        String resetLink = frontendBaseUrl + "/auth/reset-password?token=" + token;
        log.info("Password reset link for {}: {}", toEmail, resetLink);
        String subject = "Reset Your Password — CRM Platform";

        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #1976d2, #42a5f5); padding: 30px; border-radius: 12px 12px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">🔒 Password Reset</h1>
                    </div>
                    <div style="background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 12px 12px;">
                        <p style="font-size: 16px; color: #333;">Hi <strong>%s</strong>,</p>
                        <p style="font-size: 14px; color: #555; line-height: 1.6;">
                            We received a request to reset your password. Click the button below to create a new password.
                            This link will expire in <strong>30 minutes</strong>.
                        </p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s"
                               style="background-color: #1976d2; color: white; padding: 14px 32px; text-decoration: none;
                                      border-radius: 8px; font-size: 16px; font-weight: bold; display: inline-block;">
                                Reset Password
                            </a>
                        </div>
                        <p style="font-size: 13px; color: #888; line-height: 1.5;">
                            If you didn't request this, you can safely ignore this email. Your password will not be changed.
                        </p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                        <p style="font-size: 12px; color: #aaa;">
                            If the button doesn't work, copy and paste this link into your browser:<br/>
                            <a href="%s" style="color: #1976d2; word-break: break-all;">%s</a>
                        </p>
                    </div>
                </div>
                """.formatted(firstName, resetLink, resetLink, resetLink);

        sendHtmlEmail(toEmail, subject, html);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            log.warn("Email delivery failed — if testing locally, check logs for the password reset link above");
        }
    }
}
