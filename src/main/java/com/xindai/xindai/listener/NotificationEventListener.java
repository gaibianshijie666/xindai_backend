package com.xindai.xindai.listener;

import com.xindai.xindai.common.constants.QueueConstants;
import com.xindai.xindai.common.event.*;
import com.xindai.xindai.common.notification.channel.NotificationMessage;
import com.xindai.xindai.common.notification.channel.NotificationType;
import com.xindai.xindai.modules.notification.service.NotificationService;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Notification event listener that maps business events to multi-channel notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @RabbitListener(queues = QueueConstants.LOAN_APPLICATION_SUBMITTED)
    public void onApplicationSubmitted(LoanApplicationSubmittedEvent event,
                                       Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            User user = userMapper.selectById(event.getUserId());
            NotificationMessage message = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .userType("BORROWER")
                    .to(user != null ? user.getPhone() : null)
                    .subject("贷款申请已提交")
                    .content("您的贷款申请（金额：" + event.getAmount() + "元，期限：" + event.getTerm() + "期）已成功提交，请等待审核。")
                    .type(NotificationType.APPLICATION_SUBMITTED)
                    .relatedId(String.valueOf(event.getApplicationId()))
                    .params(Map.of("amount", event.getAmount().toPlainString(), "term", String.valueOf(event.getTerm())))
                    .build();
            notificationService.sendMultiChannel(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }

    @RabbitListener(queues = QueueConstants.RISK_ASSESSMENT_COMPLETED)
    public void onRiskAssessmentCompleted(RiskAssessmentCompletedEvent event,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            User user = userMapper.selectById(event.getUserId());
            NotificationMessage message = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .userType("BORROWER")
                    .to(user != null ? user.getPhone() : null)
                    .subject("风控评估完成")
                    .content("您的贷款申请风控评估已完成，风险等级：" + event.getRiskLevel() + "，信用评分：" + event.getCreditGrade())
                    .type(NotificationType.RISK_COMPLETED)
                    .relatedId(String.valueOf(event.getApplicationId()))
                    .params(Map.of(
                            "riskLevel", event.getRiskLevel(),
                            "creditGrade", String.valueOf(event.getCreditGrade())
                    ))
                    .build();
            notificationService.sendMultiChannel(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }

    @RabbitListener(queues = QueueConstants.LOAN_APPLICATION_APPROVED)
    public void onApplicationApproved(LoanApplicationApprovedEvent event,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            User user = userMapper.selectById(event.getUserId());
            NotificationMessage message = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .userType("BORROWER")
                    .to(user != null ? user.getPhone() : null)
                    .subject("贷款审批通过")
                    .content("恭喜！您的贷款申请已通过审批，审批金额：" + event.getApprovedAmount() + "元。请登录系统查看详情。")
                    .type(NotificationType.LOAN_APPROVED)
                    .relatedId(String.valueOf(event.getApplicationId()))
                    .params(Map.of(
                            "approvedAmount", event.getApprovedAmount().toPlainString(),
                            "reviewNote", event.getReviewNote() != null ? event.getReviewNote() : ""
                    ))
                    .build();
            notificationService.sendMultiChannel(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }

    @RabbitListener(queues = QueueConstants.LOAN_APPLICATION_REJECTED)
    public void onApplicationRejected(LoanApplicationRejectedEvent event,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            User user = userMapper.selectById(event.getUserId());
            NotificationMessage message = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .userType("BORROWER")
                    .to(user != null ? user.getPhone() : null)
                    .subject("贷款审批拒绝")
                    .content("很抱歉，您的贷款申请未通过审批。原因：" + event.getRejectReason())
                    .type(NotificationType.LOAN_REJECTED)
                    .relatedId(String.valueOf(event.getApplicationId()))
                    .params(Map.of("rejectReason", event.getRejectReason() != null ? event.getRejectReason() : ""))
                    .build();
            notificationService.sendMultiChannel(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }

    @RabbitListener(queues = QueueConstants.LOAN_OVERDUE_DETECTED)
    public void onOverdueDetected(LoanOverdueDetectedEvent event,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            User user = userMapper.selectById(event.getUserId());
            NotificationMessage message = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .userType("BORROWER")
                    .to(user != null ? user.getPhone() : null)
                    .subject("逾期还款通知")
                    .content("您的贷款已逾期" + event.getOverdueDays() + "天，逾期金额：" + event.getOverdueAmount() + "元。请尽快还款以避免影响信用记录。")
                    .type(NotificationType.OVERDUE_NOTICE)
                    .relatedId(String.valueOf(event.getContractId()))
                    .params(Map.of(
                            "overdueDays", String.valueOf(event.getOverdueDays()),
                            "overdueAmount", event.getOverdueAmount().toPlainString()
                    ))
                    .build();
            notificationService.sendMultiChannel(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }

    @RabbitListener(queues = QueueConstants.LOAN_REPAYMENT_COMPLETED)
    public void onRepaymentCompleted(RepaymentCompletedEvent event,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("Received event: {}, eventId: {}", event.getClass().getSimpleName(), event.getEventId());
            User user = userMapper.selectById(event.getUserId());
            NotificationMessage message = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .userType("BORROWER")
                    .to(user != null ? user.getPhone() : null)
                    .subject("还款成功")
                    .content("您的第" + event.getPeriod() + "期还款已完成，还款金额：" + event.getAmount() + "元。")
                    .type(NotificationType.REPAYMENT_COMPLETED)
                    .relatedId(String.valueOf(event.getContractId()))
                    .params(Map.of(
                            "period", String.valueOf(event.getPeriod()),
                            "amount", event.getAmount().toPlainString()
                    ))
                    .build();
            notificationService.sendMultiChannel(message);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("Failed to NACK message", ex);
            }
        }
    }
}
