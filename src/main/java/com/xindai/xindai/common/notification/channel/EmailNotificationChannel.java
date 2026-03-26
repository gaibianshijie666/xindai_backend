package com.xindai.xindai.common.notification.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    private static final Set<NotificationType> SUPPORTED_TYPES = Set.of(
            NotificationType.LOAN_APPROVED,
            NotificationType.LOAN_REJECTED,
            NotificationType.REPAYMENT_REMINDER,
            NotificationType.OVERDUE_NOTICE,
            NotificationType.CONTRACT_GENERATED
    );

    @Override
    public boolean supports(NotificationType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    @Override
    @Async
    public void send(NotificationMessage message) {
        if (message.getTo() == null || message.getTo().isBlank()) {
            log.warn("[EMAIL] No email address provided, skipping. userId={}", message.getUserId());
            return;
        }

        try {
            String templateName = resolveTemplateName(message.getType());
            Context context = new Context();
            if (message.getParams() != null) {
                message.getParams().forEach(context::setVariable);
            }
            // Always set subject as a template variable
            context.setVariable("subject", message.getSubject());

            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("[EMAIL] Sent email to: {}, subject: {}", message.getTo(), message.getSubject());
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send email to: {}, subject: {}", message.getTo(), message.getSubject(), e);
        }
    }

    private String resolveTemplateName(NotificationType type) {
        return switch (type) {
            case LOAN_APPROVED -> "loan-approved";
            case LOAN_REJECTED -> "loan-rejected";
            case REPAYMENT_REMINDER -> "repayment-reminder";
            case OVERDUE_NOTICE -> "overdue-notice";
            case CONTRACT_GENERATED -> "contract-generated";
            default -> "default";
        };
    }
}
