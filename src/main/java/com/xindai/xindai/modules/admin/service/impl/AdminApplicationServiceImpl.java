package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.ApplicationQueryDTO;
import com.xindai.xindai.modules.admin.dto.ApplicationReviewDTO;
import com.xindai.xindai.modules.admin.entity.AdminUser;
import com.xindai.xindai.modules.admin.mapper.AdminUserMapper;
import com.xindai.xindai.modules.admin.service.AdminApplicationService;
import com.xindai.xindai.modules.admin.vo.AdminApplicationVO;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.notification.service.NotificationService;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端借款申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApplicationServiceImpl implements AdminApplicationService {

    private final LoanApplicationMapper loanApplicationMapper;
    private final UserMapper userMapper;
    private final RiskAssessmentMapper riskAssessmentMapper;
    private final NotificationService notificationService;
    private final AdminUserMapper adminUserMapper;

    // 申请状态常量 - 使用ApplicationStatus枚举替代

    @Override
    public Page<AdminApplicationVO> getApplicationList(ApplicationQueryDTO queryDTO) {
        Page<LoanApplication> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<LoanApplication> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStatus() != null) {
            wrapper.eq(LoanApplication::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(LoanApplication::getCreatedAt);

        Page<LoanApplication> appPage = loanApplicationMapper.selectPage(page, wrapper);

        // 获取用户信息
        Set<Long> userIds = appPage.getRecords().stream()
                .map(LoanApplication::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 获取风险评估信息
        Set<Long> appIds = appPage.getRecords().stream()
                .map(LoanApplication::getId)
                .collect(Collectors.toSet());
        Map<Long, RiskAssessment> riskMap = appIds.isEmpty() ? Map.of() :
                riskAssessmentMapper.selectList(
                        new LambdaQueryWrapper<RiskAssessment>()
                                .in(RiskAssessment::getApplicationId, appIds)
                ).stream().collect(Collectors.toMap(RiskAssessment::getApplicationId, r -> r, (a, b) -> a));

        // 获取审核人信息
        Set<Long> reviewerIds = appPage.getRecords().stream()
                .map(LoanApplication::getReviewerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, AdminUser> reviewerMap = reviewerIds.isEmpty() ? Map.of() :
                adminUserMapper.selectBatchIds(reviewerIds).stream()
                        .collect(Collectors.toMap(AdminUser::getId, u -> u));

        // 转换为VO
        Page<AdminApplicationVO> voPage = new Page<>(appPage.getCurrent(), appPage.getSize(), appPage.getTotal());
        List<AdminApplicationVO> voList = appPage.getRecords().stream()
                .map(app -> convertToVO(app, userMap.get(app.getUserId()), riskMap.get(app.getId()), reviewerMap.get(app.getReviewerId())))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long id, ApplicationReviewDTO reviewDTO) {
        LoanApplication application = loanApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }

        // 检查状态是否可以审核
        if (application.getStatus() != ApplicationStatus.PENDING.getCode() && application.getStatus() != ApplicationStatus.REVIEWING.getCode()) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_REVIEWED);
        }

        // 获取当前审核人ID
        Long reviewerId = getCurrentAdminId();

        // 确定审批动作
        String action = reviewDTO.getAction();
        if (action == null || action.isBlank()) {
            action = reviewDTO.getApproved() ? "APPROVED" : "REJECTED";
        }

        // 确定状态和备注
        int newStatus;
        String reviewNote = reviewDTO.getReason();

        switch (action) {
            case "APPROVED":
                newStatus = ApplicationStatus.APPROVED.getCode();
                break;
            case "REJECTED":
                newStatus = ApplicationStatus.REJECTED.getCode();
                break;
            case "RETURNED":
                newStatus = ApplicationStatus.PENDING.getCode();
                if (reviewNote == null || reviewNote.isBlank()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "退回补充材料必须填写退回原因");
                }
                break;
            case "CONDITIONAL":
                newStatus = ApplicationStatus.APPROVED.getCode();
                if (reviewNote == null || reviewNote.isBlank()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "条件性通过必须记录审批条件");
                }
                break;
            default:
                throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的审批动作: " + action);
        }

        LambdaUpdateWrapper<LoanApplication> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LoanApplication::getId, id)
                .set(LoanApplication::getStatus, newStatus)
                .set(LoanApplication::getReviewerId, reviewerId)
                .set(LoanApplication::getReviewNote, reviewNote)
                .set(LoanApplication::getReviewedAt, LocalDateTime.now());

        loanApplicationMapper.update(null, wrapper);
        log.info("审核借款申请: applicationId={}, action={}, reviewerId={}, reviewNote={}",
                id, action, reviewerId, reviewNote);

        // 发送通知
        String resultText;
        switch (action) {
            case "APPROVED":
                resultText = "已通过";
                break;
            case "REJECTED":
                resultText = "已拒绝";
                break;
            case "RETURNED":
                resultText = "已退回，请补充材料";
                break;
            case "CONDITIONAL":
                resultText = "已条件性通过";
                break;
            default:
                resultText = "已处理";
        }
        String reasonText = reviewNote != null ? "，审核意见：" + reviewNote : "";
        notificationService.send(application.getUserId(), "USER", "贷款申请审核结果",
                "您的贷款申请（编号：" + application.getApplicationNo() + "）" + resultText + reasonText + "。",
                "LOAN", application.getId().toString());
    }

    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    private AdminApplicationVO convertToVO(LoanApplication app, User user, RiskAssessment risk, AdminUser reviewer) {
        AdminApplicationVO vo = new AdminApplicationVO();
        vo.setId(app.getId());
        vo.setApplicationNo(app.getApplicationNo());
        vo.setUserId(app.getUserId());
        vo.setUserName(user != null ? user.getRealName() : null);
        vo.setAmount(app.getAmount());
        vo.setTerm(app.getTerm());
        vo.setPurpose(app.getPurpose());
        vo.setStatus(app.getStatus());
        vo.setRiskScore(risk != null ? risk.getRiskScore() : null);
        vo.setCreatedAt(app.getCreatedAt());
        vo.setReviewedAt(app.getReviewedAt());
        vo.setReviewerId(app.getReviewerId());
        vo.setReviewerName(reviewer != null ? reviewer.getRealName() : null);
        vo.setReviewNote(app.getReviewNote());
        return vo;
    }
}
