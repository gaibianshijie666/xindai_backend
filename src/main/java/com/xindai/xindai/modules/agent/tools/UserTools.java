package com.xindai.xindai.modules.agent.tools;

import com.xindai.xindai.common.util.DesensitizeUtils;
import com.xindai.xindai.modules.agent.annotation.ToolAllowed;
import com.xindai.xindai.modules.user.dto.UserProfileVO;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.service.UserProfileDetailService;
import com.xindai.xindai.modules.user.service.UserProfileService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTools {

    private final UserProfileService userProfileService;
    private final UserProfileDetailService userProfileDetailService;

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("获取当前登录用户的基本信息，包括手机号、姓名、状态等")
    public String getUserInfo() {
        try {
            User user = userProfileService.getById(ToolContext.getUserId());
            if (user == null) return "未找到用户信息";
            return String.format(
                    "用户ID: %d, 手机号: %s, 姓名: %s, 状态: %s, 创建时间: %s",
                    user.getId(),
                    DesensitizeUtils.maskPhone(user.getPhone()),
                    user.getRealName() != null ? user.getRealName() : "未实名",
                    user.getStatus() == 1 ? "正常" : "禁用",
                    user.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("Tool getUserInfo failed", e);
            return "查询用户信息失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("获取当前用户的详细画像信息，包括信用评分、信用等级、年收入、债务收入比等")
    public String getUserProfile() {
        try {
            UserProfileVO profile = userProfileDetailService.getProfileDetail(ToolContext.getUserId());
            return String.format(
                    "信用评分: %d, 信用等级: %s, 年收入: %.0f元, 债务收入比: %.2f, 就业年限: %d年, 风险评分: %.1f",
                    profile.getCreditScore() != null ? profile.getCreditScore() : 0,
                    profile.getCreditGrade() != null ? profile.getCreditGrade() : "N/A",
                    profile.getAnnualIncome() != null ? profile.getAnnualIncome() : 0,
                    profile.getDti() != null ? profile.getDti() : 0,
                    profile.getEmploymentYears() != null ? profile.getEmploymentYears() : 0,
                    profile.getRiskScore() != null ? profile.getRiskScore() : 0
            );
        } catch (Exception e) {
            log.error("Tool getUserProfile failed", e);
            return "查询用户画像失败: " + e.getMessage();
        }
    }
}
