package com.xindai.xindai.modules.risk.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.risk.dto.RiskAssessmentVO;
import com.xindai.xindai.modules.risk.entity.Blacklist;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.service.BlacklistService;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "风控管理", description = "风险评估、黑名单管理等接口")
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RiskController {

    private final RiskAssessmentService riskAssessmentService;
    private final BlacklistService blacklistService;

    @Operation(
            summary = "执行风险评估",
            description = "对指定用户进行智能风控评估，返回风险评分、风险等级和决策建议"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "评估完成",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "success",
                                      "data": {
                                        "assessmentNo": "RA123456789",
                                        "userId": 1,
                                        "riskScore": 35.5,
                                        "riskLevel": 2,
                                        "decision": "MANUAL_REVIEW",
                                        "modelVersion": "v2.0",
                                        "processingTimeMs": 150,
                                        "factors": [
                                          {"name": "fico_score", "impact": 0.2, "direction": "positive"}
                                        ]
                                      }
                                    }
                                    """)
                    ))
    })
    @PostMapping("/assess")
    public Result<RiskAssessmentVO> assess(
            @Parameter(description = "用户ID", required = true, example = "1")
            @RequestParam Long userId,
            @Parameter(description = "借款申请ID（可选）", example = "100")
            @RequestParam(required = false) Long applicationId,
            @Parameter(description = "评估类型：1-贷前，2-贷中，3-贷后", example = "2")
            @RequestParam(defaultValue = "2") Integer assessmentType) {

        var assessment = riskAssessmentService.assess(userId, applicationId, assessmentType);
        return Result.success(riskAssessmentService.toVO(assessment));
    }

    @Operation(
            summary = "获取评估结果",
            description = "根据评估编号获取风险评估的详细结果"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "评估记录不存在")
    })
    @GetMapping("/assessments/{assessmentNo}")
    public Result<RiskAssessmentVO> getAssessment(
            @Parameter(description = "评估编号", required = true, example = "RA123456789")
            @PathVariable String assessmentNo) {
        var assessment = riskAssessmentService.getByAssessmentNo(assessmentNo);
        if (assessment == null) {
            return Result.error("评估记录不存在");
        }
        return Result.success(riskAssessmentService.toVO(assessment));
    }

    @Operation(
            summary = "获取用户评估历史",
            description = "分页获取指定用户的风险评估历史记录"
    )
    @GetMapping("/history/{userId}")
    public Result<Page<RiskAssessmentVO>> getHistory(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<RiskAssessment> assessmentPage = riskAssessmentService.getAssessmentHistory(userId, page, size);

        Page<RiskAssessmentVO> voPage = new Page<>(assessmentPage.getCurrent(), assessmentPage.getSize(), assessmentPage.getTotal());
        List<RiskAssessmentVO> voList = assessmentPage.getRecords().stream()
                .map(riskAssessmentService::toVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }

    @Operation(
            summary = "获取黑名单列表",
            description = "获取系统黑名单列表，可按类型筛选"
    )
    @GetMapping("/blacklist")
    public Result<List<Blacklist>> getBlacklist(
            @Parameter(description = "黑名单类型：1-手机号，2-身份证，不传则返回全部")
            @RequestParam(required = false) Integer type) {
        return Result.success(blacklistService.getBlacklist(type));
    }

    @Operation(
            summary = "添加黑名单",
            description = "将指定手机号或身份证添加到黑名单"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/blacklist")
    public Result<Void> addBlacklist(
            @Parameter(description = "类型：1-手机号，2-身份证", required = true, example = "1")
            @RequestParam Integer type,
            @Parameter(description = "值（手机号或身份证号）", required = true, example = "13800138000")
            @RequestParam String value,
            @Parameter(description = "加入原因", example = "恶意逾期")
            @RequestParam(required = false) String reason) {
        blacklistService.addToBlacklist(type, value, reason);
        return Result.success();
    }

    @Operation(
            summary = "移除黑名单",
            description = "从黑名单中移除指定记录"
    )
    @DeleteMapping("/blacklist")
    public Result<Void> removeBlacklist(
            @Parameter(description = "类型：1-手机号，2-身份证", required = true, example = "1")
            @RequestParam Integer type,
            @Parameter(description = "值（手机号或身份证号）", required = true, example = "13800138000")
            @RequestParam String value) {
        blacklistService.removeFromBlacklist(type, value);
        return Result.success();
    }
}
