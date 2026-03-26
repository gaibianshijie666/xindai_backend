# 信贷系统非安全问题全面修复计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复信贷系统中除安全层面外的所有业务逻辑、数据层、架构层、用户体验和运维层面的问题

**Architecture:** 分4个阶段并行推进：阶段1修复Bug和基础设施，阶段2完善数据层，阶段3增强业务逻辑，阶段4新增功能模块。各阶段内的独立任务可完全并行。

**Tech Stack:** Spring Boot 3.5.11, Java 17, MyBatis-Plus 3.5.5, MySQL, Redis, LangChain4j 0.36.2

---

## 已发现的额外严重问题

在分析过程中发现了之前未识别的关键Bug：

| # | 问题 | 严重程度 |
|---|------|---------|
| B1 | **LoanApplication status值冲突**: LoanServiceImpl(0=待审,1=自动通过,2=人工复核,3=自动拒绝) vs AdminApplicationServiceImpl(0=待审,1=审核中,2=通过,3=拒绝) | 严重 |
| B2 | **LoanContract status值冲突**: LoanServiceImpl(status=2=已结清) vs AdminDashboardServiceImpl(CONTRACT_STATUS_OVERDUE=2=逾期) | 严重 |
| B3 | **EnterpriseTools.mapStatus()映射不一致**: 0=待审,1=通过,2=拒绝,3=已放款 vs Admin(0=待审,1=审核中,2=通过,3=拒绝) | 中等 |
| B4 | **AdminDashboardServiceImpl.calculateDailyStats()固定返回7天数据**,30d/90d查询时也只返回7天 | 中等 |
| B5 | **AdminDashboardServiceImpl加载全量数据到内存**计算贷款总额，性能隐患 | 低 |

---

## 阶段1: Bug修复与基础设施 (可并行)

### Task 1: 统一LoanApplication状态值

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/entity/LoanApplication.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminApplicationServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/enterprise/service/impl/EnterpriseLoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/LoanTools.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/EnterpriseTools.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/enums/ApplicationStatus.java`

- [ ] **Step 1: 创建ApplicationStatus枚举**

统一状态值为：0=待审批(PENDING), 1=审批中(REVIEWING), 2=已通过(APPROVED), 3=已拒绝(REJECTED), 4=已放款(DISBURSED), 5=已结清(SETTLED)

```java
package com.xindai.xindai.modules.loan.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING(0, "待审批"),
    REVIEWING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    DISBURSED(4, "已放款"),
    SETTLED(5, "已结清");

    private final int code;
    private final String description;

    ApplicationStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ApplicationStatus fromCode(int code) {
        for (ApplicationStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown ApplicationStatus code: " + code);
    }
}
```

- [ ] **Step 2: 修改LoanServiceImpl.apply()方法中的状态逻辑**

将原来的状态映射修改为统一枚举值：
- 风控决策APPROVE -> status = `ApplicationStatus.APPROVED.getCode()` (2)
- 风控决策MANUAL_REVIEW -> status = `ApplicationStatus.REVIEWING.getCode()` (1)
- 风控决策REJECT -> status = `ApplicationStatus.REJECTED.getCode()` (3)

- [ ] **Step 3: 修改AdminApplicationServiceImpl中的状态常量**

替换常量为枚举引用：
- `STATUS_PENDING` -> `ApplicationStatus.PENDING.getCode()`
- `STATUS_REVIEWING` -> `ApplicationStatus.REVIEWING.getCode()`
- `STATUS_APPROVED` -> `ApplicationStatus.APPROVED.getCode()`
- `STATUS_REJECTED` -> `ApplicationStatus.REJECTED.getCode()`

- [ ] **Step 4: 修改EnterpriseLoanServiceImpl中的状态引用**

确保企业端贷款申请创建时使用 `ApplicationStatus.PENDING.getCode()`，审核时使用 `APPROVED`/`REJECTED`。

- [ ] **Step 5: 修改LoanTools和EnterpriseTools中的状态映射**

更新mapStatus方法使用统一的ApplicationStatus枚举。

- [ ] **Step 6: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 2: 统一LoanContract和RepaymentPlan状态值

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/entity/LoanContract.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/entity/RepaymentPlan.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminDashboardServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/LoanTools.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/enums/ContractStatus.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/enums/RepaymentStatus.java`

- [ ] **Step 1: 创建ContractStatus枚举**

统一合同状态：0=待放款(PENDING), 1=还款中(REPAYING), 2=已结清(SETTLED), 3=逾期(OVERDUE)

```java
package com.xindai.xindai.modules.loan.enums;

import lombok.Getter;

@Getter
public enum ContractStatus {
    PENDING(0, "待放款"),
    REPAYING(1, "还款中"),
    SETTLED(2, "已结清"),
    OVERDUE(3, "逾期");

    private final int code;
    private final String description;

    ContractStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 2: 创建RepaymentStatus枚举**

统一还款计划状态：0=待还款(PENDING), 1=已还款(PAID), 2=已逾期(OVERDUE)

- [ ] **Step 3: 修改LoanServiceImpl中的合同/还款状态引用**

将合同状态从硬编码的1/2改为使用ContractStatus枚举，还款计划状态从0/1改为使用RepaymentStatus枚举。

- [ ] **Step 4: 修改AdminDashboardServiceImpl**

将 `CONTRACT_STATUS_OVERDUE = 2` 改为 `ContractStatus.OVERDUE.getCode()` (3)。修复逾期率计算逻辑。

- [ ] **Step 5: 修复calculateDailyStats()方法**

改为根据range参数动态计算天数：7d=7天, 30d=30天, 90d=90天。

- [ ] **Step 6: 修改LoanTools中的状态映射**

使用统一的ContractStatus和RepaymentStatus枚举。

- [ ] **Step 7: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 3: 规范化分页参数

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/dto/BasePageDTO.java`
- Modify: 所有使用分页参数的Controller和DTO

- [ ] **Step 1: 创建BasePageDTO基类**

```java
package com.xindai.xindai.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class BasePageDTO {
    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer size = 10;
}
```

- [ ] **Step 2: 找出所有手动接收page/size参数的Controller端点**

需要修改的文件：
- `EnterpriseCustomerController` - list方法使用`@RequestParam int page, @RequestParam int size`
- `EnterpriseLoanController` - list方法
- `EnterpriseRiskController` - history方法
- `EnterpriseDashboardController` - trend方法
- `AdminUserController` - getUserList使用UserQueryDTO
- `AdminApplicationController` - getApplicationList使用ApplicationQueryDTO
- `AdminBlacklistController` - getBlacklist使用BlacklistQueryDTO

- [ ] **Step 3: 让已有DTO继承BasePageDTO**

修改以下DTO继承BasePageDTO：
- `UserQueryDTO` extends `BasePageDTO`
- `ApplicationQueryDTO` extends `BasePageDTO`
- `BlacklistQueryDTO` extends `BasePageDTO`

- [ ] **Step 4: 为企业端Controller创建查询DTO继承BasePageDTO**

- `EnterpriseCustomerQueryDTO` extends `BasePageDTO` (含keyword字段)
- `EnterpriseLoanQueryDTO` extends `BasePageDTO` (含status字段)
- `EnterpriseRiskHistoryQueryDTO` extends `BasePageDTO` (含customerId字段)
- `EnterpriseTrendQueryDTO` (含startDate, endDate字段，不含分页)

- [ ] **Step 5: 修改Controller方法签名和Service方法**

将Controller的`@RequestParam int page, @RequestParam int size`替换为DTO参数，相应修改Service方法签名。

- [ ] **Step 6: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 4: 限制Actuator端点暴露

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 修改Actuator配置**

在application.yml中将actuator暴露端点从`health,info,metrics,loggers`改为仅`health,info,metrics`，移除loggers端点（生产环境不应暴露日志级别修改能力）。

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

### Task 5: 添加结构化日志与TraceId

**Files:**
- Modify: `src/main/resources/logback-spring.xml`
- Create: `src/main/java/com/xindai/xindai/common/filter/TraceFilter.java`
- Modify: `src/main/java/com/xindai/xindai/config/WebMvcConfig.java`

- [ ] **Step 1: 创建TraceFilter**

```java
package com.xindai.xindai.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter implements Filter {
    private static final String TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String traceId = httpRequest.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_ID, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

- [ ] **Step 2: 修改logback-spring.xml日志格式**

在CONSOLE和FILE appender的pattern中添加`[%X{traceId}]`：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
```

- [ ] **Step 3: 在GlobalExceptionHandler中返回traceId**

修改catch-all Exception处理，在Result中添加traceId信息：

```java
@ExceptionHandler(Exception.class)
public Result<Void> handleException(Exception e) {
    log.error("系统异常", e);
    String traceId = MDC.get("traceId");
    return Result.error(500, "系统繁忙，请稍后重试" + (traceId != null ? " (traceId: " + traceId + ")" : ""));
}
```

- [ ] **Step 4: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

## 阶段2: 数据层完善 (可并行)

### Task 6: 引入Flyway数据库迁移

**Files:**
- Modify: `pom.xml` - 添加Flyway依赖
- Modify: `src/main/resources/application.yml` - 添加Flyway配置
- Create: `src/main/resources/db/migration/V1__init_schema.sql` - 基于现有schema.sql
- Create: `src/main/resources/db/migration/V2__enterprise_schema.sql` - 基于现有schema_enterprise.sql
- Create: `src/main/resources/db/migration/V3__profile_fields.sql` - 基于现有migration_add_profile_fields.sql

- [ ] **Step 1: 添加Flyway依赖到pom.xml**

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

- [ ] **Step 2: 添加Flyway配置到application.yml**

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    table: flyway_schema_history
```

- [ ] **Step 3: 将现有SQL脚本迁移到Flyway格式**

将`schema.sql`内容复制到`V1__init_schema.sql`，`schema_enterprise.sql`到`V2__enterprise_schema.sql`，`migration_add_profile_fields.sql`到`V3__profile_fields.sql`。注意：需要确保V1-V3的DDL语句使用`IF NOT EXISTS`以兼容baseline-on-migrate。

- [ ] **Step 4: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 7: 添加数据脱敏工具

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/util/DesensitizeUtils.java`
- Create: `src/main/java/com/xindai/xindai/common/annotation/Desensitize.java`
- Create: `src/main/java/com/xindai/xindai/common/serializer/DesensitizeSerializer.java`
- Modify: `src/main/java/com/xindai/xindai/modules/user/dto/UserProfileVO.java` - 对敏感字段添加注解
- Modify: `src/main/java/com/xindai/xindai/modules/user/vo/UserVO.java` - 对phone/idCard添加注解

- [ ] **Step 1: 创建Desensitize注解**

```java
package com.xindai.xindai.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.xindai.xindai.common.serializer.DesensitizeSerializer;

import java.lang.annotation.*;

@JacksonAnnotationsInside
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {
    DesensitizeType value();
    enum DesensitizeType {
        PHONE,    // 138****1234
        ID_CARD,  // 110***********1234
        NAME,     // 张*
        BANK_CARD // 6222 **** **** 1234
    }
}
```

- [ ] **Step 2: 创建DesensitizeSerializer**

```java
package com.xindai.xindai.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.xindai.xindai.common.annotation.Desensitize;

import java.io.IOException;

public class DesensitizeSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        // 根据注解类型脱敏
        gen.writeString(DesensitizeUtils.mask(value));
    }
}
```

注意：Serializer需要获取字段的注解类型来决定脱敏方式。可以通过自定义BeanSerializerModifier或ContextualSerializer实现。

实际实现方案：使用Jackson的`@JsonSerialize(using = ...)`配合`ContextualSerializer`，在序列化时获取字段上的`@Desensitize`注解类型。

- [ ] **Step 3: 创建DesensitizeUtils工具类**

```java
package com.xindai.xindai.common.util;

public class DesensitizeUtils {
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 6) + "***********" + idCard.substring(idCard.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
```

- [ ] **Step 4: 在VO类上应用脱敏注解**

在`UserVO`的phone字段添加`@Desensitize(DesensitizeType.PHONE)`，idCard字段添加`@Desensitize(DesensitizeType.ID_CARD)`。在`UserProfileVO`的类似字段也添加。

- [ ] **Step 5: 移除AdminUserServiceImpl中手动的脱敏代码**

`AdminUserServiceImpl.getUserDetail()`中有手动idCard脱敏代码(idCard.substring(0,6) + "****" + ...)，改为使用注解自动脱敏，移除手动代码。

- [ ] **Step 6: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 8: 添加操作审计日志 (AOP)

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/annotation/OperateLog.java`
- Create: `src/main/java/com/xindai/xindai/common/entity/OperationLog.java`
- Create: `src/main/java/com/xindai/xindai/common/mapper/OperationLogMapper.java`
- Create: `src/main/java/com/xindai/xindai/common/aspect/OperateLogAspect.java`
- Create: `src/main/resources/db/migration/V4__operation_log.sql`
- Modify: 多个Controller方法添加@OperateLog注解

- [ ] **Step 1: 创建operation_log表DDL**

```sql
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '操作人ID',
    user_type VARCHAR(20) COMMENT '操作人类型: USER/ADMIN/ENTERPRISE',
    username VARCHAR(50) COMMENT '操作人用户名',
    module VARCHAR(50) COMMENT '业务模块',
    operation VARCHAR(100) COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id VARCHAR(50) COMMENT '目标ID',
    detail TEXT COMMENT '操作详情',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    trace_id VARCHAR(32) COMMENT '链路追踪ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_module (module),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
```

- [ ] **Step 2: 创建@OperateLog注解**

```java
package com.xindai.xindai.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {
    String module() default "";
    String operation() default "";
}
```

- [ ] **Step 3: 创建OperationLog实体和Mapper**

使用MyBatis-Plus注解映射到operation_log表。

- [ ] **Step 4: 创建OperateLogAspect切面**

```java
@Aspect
@Component
@Slf4j
public class OperateLogAspect {
    @Autowired
    private OperationLogMapper operationLogMapper;

    @AfterReturning("@annotation(operateLog)")
    public void recordLog(JoinPoint joinPoint, OperateLog operateLog) {
        // 从SecurityContext获取当前用户信息
        // 从RequestContextHolder获取IP
        // 从MDC获取traceId
        // 异步保存日志
    }
}
```

使用`@Async`异步写入避免影响业务接口性能。

- [ ] **Step 5: 在关键Controller方法上添加注解**

在以下方法添加@OperateLog：
- AdminUserController: getUserList, updateUserStatus
- AdminApplicationController: reviewApplication
- AdminBlacklistController: addBlacklist, removeBlacklist
- EnterpriseCustomerController: create, update, delete, batchImport, batchDelete
- EnterpriseLoanController: apply, batchApply, batchReview
- UserProfileController: updateProfile, changePassword, verifyIdentity
- LoanController: apply, repay, repayByPeriod, applyLimitIncrease

- [ ] **Step 6: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

## 阶段3: 业务逻辑增强 (部分可并行)

### Task 9: 添加逾期管理与罚息计算

**Files:**
- Create: `src/main/java/com/xindai/xindai/modules/loan/service/OverdueService.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/service/impl/OverdueServiceImpl.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/task/OverdueCheckTask.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/dto/OverdueInfoVO.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/entity/RepaymentPlan.java` - 添加penaltyAmount字段
- Modify: `src/main/java/com/xindai/xindai/modules/loan/entity/LoanContract.java` - 使用ContractStatus枚举
- Create: `src/main/resources/db/migration/V5__overdue_fields.sql`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/controller/LoanController.java` - 添加逾期查询接口
- Modify: `src/main/resources/application.yml` - 启用scheduling

- [ ] **Step 1: 创建DDL添加罚息字段**

```sql
ALTER TABLE repayment_plan ADD COLUMN penalty_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '罚息金额';
ALTER TABLE repayment_plan ADD COLUMN overdue_days INT DEFAULT 0 COMMENT '逾期天数';
```

- [ ] **Step 2: 修改RepaymentPlan实体添加penaltyAmount和overdueDays字段**

- [ ] **Step 3: 创建OverdueService接口和实现**

核心方法：
- `void checkAndMarkOverdue()` - 定时检查并标记逾期还款计划
- `BigDecimal calculatePenalty(RepaymentPlan plan)` - 计算罚息（逾期本金 × 日罚息率 × 逾期天数，日罚息率默认万分之五）
- `List<OverdueInfoVO> getOverdueList(Long userId)` - 获取用户逾期列表
- `void updateOverdueStatus()` - 将连续逾期超过90天的合同标记为严重逾期

- [ ] **Step 4: 创建OverdueCheckTask定时任务**

```java
@Component
@EnableScheduling
public class OverdueCheckTask {
    @Autowired
    private OverdueService overdueService;

    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void checkOverdue() {
        log.info("开始执行逾期检查任务");
        overdueService.checkAndMarkOverdue();
    }
}
```

- [ ] **Step 5: 创建OverdueInfoVO**

```java
@Data
public class OverdueInfoVO {
    private Long contractId;
    private String contractNo;
    private Integer period;
    private LocalDate dueDate;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal penaltyAmount;
    private Integer overdueDays;
    private BigDecimal totalOverdueAmount; // principal + interest + penalty
}
```

- [ ] **Step 6: 在LoanController添加逾期查询接口**

```java
@GetMapping("/overdue")
@Operation(summary = "获取逾期还款列表")
public Result<List<OverdueInfoVO>> getOverdueList() { ... }
```

- [ ] **Step 7: 在application.yml中启用scheduling**

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 2
```

- [ ] **Step 8: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 10: 增强贷款审批流程

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/admin/dto/ApplicationReviewDTO.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminApplicationServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/entity/LoanApplication.java` - 添加reviewerId和reviewNote字段
- Create: `src/main/resources/db/migration/V6__application_review_fields.sql`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/vo/AdminApplicationVO.java` - 显示审核信息

- [ ] **Step 1: 扩展ApplicationReviewDTO**

```java
@Data
public class ApplicationReviewDTO {
    @NotNull
    private Boolean approved;

    private String reason;

    /**
     * 审批动作：APPROVED-通过, REJECTED-拒绝, RETURNED-退回补充材料, CONDITIONAL-条件性通过
     */
    private String action; // 默认根据approved自动推断
}
```

- [ ] **Step 2: 添加DDL字段**

```sql
ALTER TABLE loan_application ADD COLUMN reviewer_id BIGINT COMMENT '审核人ID';
ALTER TABLE loan_application ADD COLUMN review_note TEXT COMMENT '审核备注';
```

- [ ] **Step 3: 修改AdminApplicationServiceImpl.reviewApplication()**

支持4种审核动作：
- `APPROVED`: 通过，状态改为2
- `REJECTED`: 拒绝，状态改为3
- `RETURNED`: 退回补充材料，状态改为0（重新变为待审批），保存审核备注
- `CONDITIONAL`: 条件性通过（降低额度），状态改为2，在reviewNote中记录条件

- [ ] **Step 4: 修改LoanApplication实体添加reviewerId和reviewNote字段**

- [ ] **Step 5: 修改AdminApplicationVO显示审核信息**

添加reviewerName、reviewNote、reviewedAt字段。

- [ ] **Step 6: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 11: 企业端额度管理

**Files:**
- Create: `src/main/java/com/xindai/xindai/modules/enterprise/controller/EnterpriseCreditController.java`
- Create: `src/main/java/com/xindai/xindai/modules/enterprise/service/EnterpriseCreditService.java`
- Create: `src/main/java/com/xindai/xindai/modules/enterprise/service/impl/EnterpriseCreditServiceImpl.java`
- Create: `src/main/java/com/xindai/xindai/modules/enterprise/dto/CreditApplyDTO.java`
- Create: `src/main/java/com/xindai/xindai/modules/enterprise/dto/CreditInfoVO.java`
- Modify: `src/main/java/com/xindai/xindai/common/exception/ErrorCode.java` - 添加相关错误码

- [ ] **Step 1: 添加错误码**

```java
// Enterprise module
ENTERPRISE_CREDIT_APPLY_PENDING(4007, "存在待审批的额度申请"),
ENTERPRISE_CREDIT_APPLY_REJECTED(4008, "额度申请被拒绝，请30天后重试"),
```

- [ ] **Step 2: 创建CreditApplyDTO**

```java
@Data
public class CreditApplyDTO {
    @NotNull
    @DecimalMin("100000")
    @DecimalMax("10000000")
    private BigDecimal requestedLimit;

    private String reason;
}
```

- [ ] **Step 3: 创建CreditInfoVO**

```java
@Data
public class CreditInfoVO {
    private BigDecimal creditLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;
    private LocalDateTime expireAt;
}
```

- [ ] **Step 4: 创建EnterpriseCreditService**

方法：
- `CreditInfoVO getCreditInfo(Long enterpriseId)` - 查询额度信息
- `void applyCreditIncrease(Long enterpriseId, CreditApplyDTO dto)` - 申请提额

- [ ] **Step 5: 创建EnterpriseCreditController**

```java
@RestController
@RequestMapping("/api/v1/enterprise/credit")
@Tag(name = "企业端-额度管理")
public class EnterpriseCreditController {
    @GetMapping
    @PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN', 'ENTERPRISE_OPERATOR')")
    public Result<CreditInfoVO> getCreditInfo() { ... }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('ENTERPRISE_ADMIN')")
    public Result<Void> applyCreditIncrease(@RequestBody @Valid CreditApplyDTO dto) { ... }
}
```

- [ ] **Step 6: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 12: 改进信用额度计算器

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/CreditLimitCalculatorImpl.java` (如果存在)
- Modify: `src/main/java/com/xindai/xindai/config/CreditLimitProperties.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 查找CreditLimitCalculator的实现类**

搜索`CreditLimitCalculator`接口的实现类，读取完整代码。

- [ ] **Step 2: 增强额度计算逻辑**

在现有计算基础上增加以下因子：
- 信用等级调整系数（A=1.5, B=1.3, C=1.1, D=1.0, E=0.8, F=0.6, G=0.4）
- 就业年限调整（<1年=0.8, 1-3年=0.9, 3-5年=1.0, 5-10年=1.1, >10年=1.2）
- DTI调整（<30%=1.0, 30-50%=0.9, 50-70%=0.7, >70%=0.5）
- 历史还款记录因子（如果用户有结清合同且无逾期，增加10-20%额度）

- [ ] **Step 3: 添加计算因子配置到application.yml**

```yaml
credit-limit:
  grade-multipliers:
    A: 1.5
    B: 1.3
    C: 1.1
    D: 1.0
    E: 0.8
    F: 0.6
    G: 0.4
  employment-year-multipliers:
    less-than-1: 0.8
    1-to-3: 0.9
    3-to-5: 1.0
    5-to-10: 1.1
    over-10: 1.2
  dti-adjustments:
    low: 1.0      # <30%
    medium: 0.9   # 30-50%
    high: 0.7     # 50-70%
    very-high: 0.5 # >70%
```

- [ ] **Step 4: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 13: 修复AdminDashboardServiceImpl性能问题

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminDashboardServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/mapper/` 相关Mapper - 添加聚合查询

- [ ] **Step 1: 在Mapper中添加聚合查询方法**

在`LoanApplicationMapper`中添加SQL聚合查询，避免全量加载：
```java
@Select("SELECT COALESCE(SUM(amount), 0) FROM loan_application WHERE status = 2 AND deleted = 0")
BigDecimal sumApprovedAmount();
```

- [ ] **Step 2: 修改AdminDashboardServiceImpl使用聚合查询**

替换原有的全量加载逻辑为Mapper聚合查询。

- [ ] **Step 3: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

## 阶段4: 新功能模块 (可并行)

### Task 14: 文件上传服务

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/config/StorageProperties.java`
- Create: `src/main/java/com/xindai/xindai/modules/file/controller/FileController.java`
- Create: `src/main/java/com/xindai/xindai/modules/file/service/FileStorageService.java`
- Create: `src/main/java/com/xindai/xindai/modules/file/service/impl/FileStorageServiceImpl.java`
- Create: `src/main/java/com/xindai/xindai/modules/file/dto/FileUploadVO.java`
- Modify: `src/main/resources/application.yml` - 添加文件上传配置
- Modify: `src/main/java/com/xindai/xindai/security/config/SecurityConfig.java` - 允许上传端点

- [ ] **Step 1: 添加文件上传配置**

```yaml
file:
  upload:
    path: ./uploads
    max-size: 10MB
    allowed-types: image/jpeg,image/png,image/gif,application/pdf
```

- [ ] **Step 2: 创建StorageProperties配置类**

- [ ] **Step 3: 创建FileStorageService**

方法：
- `FileUploadVO upload(MultipartFile file, String bizType)` - 上传文件
- `void delete(String filePath)` - 删除文件
- `Resource load(String filePath)` - 加载文件

文件命名策略: `{bizType}/{yyyy/MM/dd}/{UUID}.{ext}`

- [ ] **Step 4: 创建FileUploadVO**

```java
@Data
public class FileUploadVO {
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
}
```

- [ ] **Step 5: 创建FileController**

```java
@RestController
@RequestMapping("/api/v1/file")
@Tag(name = "文件管理")
public class FileController {
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "general") String bizType) { ... }
}
```

- [ ] **Step 6: 在SecurityConfig中配置上传端点的认证**

`/api/v1/file/**`需要JWT认证。

- [ ] **Step 7: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 15: 消息通知中心

**Files:**
- Create: `src/main/java/com/xindai/xindai/modules/notification/entity/Notification.java`
- Create: `src/main/java/com/xindai/xindai/modules/notification/mapper/NotificationMapper.java`
- Create: `src/main/java/com/xindai/xindai/modules/notification/service/NotificationService.java`
- Create: `src/main/java/com/xindai/xindai/modules/notification/service/impl/NotificationServiceImpl.java`
- Create: `src/main/java/com/xindai/xindai/modules/notification/controller/NotificationController.java`
- Create: `src/main/java/com/xindai/xindai/modules/notification/dto/NotificationVO.java`
- Create: `src/main/java/com/xindai/xindai/modules/notification/dto/NotificationQueryDTO.java`
- Create: `src/main/resources/db/migration/V7__notification.sql`

- [ ] **Step 1: 创建通知表DDL**

```sql
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '接收人ID',
    user_type VARCHAR(20) COMMENT 'USER/ADMIN/ENTERPRISE',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type VARCHAR(30) COMMENT 'SYSTEM/RISK/LOAN/PAYMENT/ENTERPRISE',
    is_read TINYINT DEFAULT 0 COMMENT '0=未读,1=已读',
    related_id VARCHAR(50) COMMENT '关联业务ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME COMMENT '阅读时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知';
```

- [ ] **Step 2: 创建Notification实体和Mapper**

- [ ] **Step 3: 创建NotificationService**

方法：
- `void send(Long userId, String userType, String title, String content, String type, String relatedId)` - 发送通知
- `PageResult<NotificationVO> list(Long userId, NotificationQueryDTO query)` - 查询通知列表
- `void markRead(Long userId, Long notificationId)` - 标记已读
- `void markAllRead(Long userId)` - 全部标记已读
- `long getUnreadCount(Long userId)` - 获取未读数量

- [ ] **Step 4: 创建NotificationController**

```java
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "消息通知")
public class NotificationController {
    @GetMapping
    public Result<PageResult<NotificationVO>> list(NotificationQueryDTO query) { ... }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() { ... }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) { ... }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() { ... }
}
```

- [ ] **Step 5: 在业务关键节点注入通知发送**

在以下位置添加通知发送调用：
- `LoanServiceImpl.apply()` - 贷款申请提交成功后发送通知
- `AdminApplicationServiceImpl.reviewApplication()` - 审核完成后通知用户
- `LoanServiceImpl.executeRepayment()` - 还款成功后发送通知
- `OverdueServiceImpl.checkAndMarkOverdue()` - 逾期时发送提醒通知

- [ ] **Step 6: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

### Task 16: AI Agent工具权限隔离

**Files:**
- Create: `src/main/java/com/xindai/xindai/modules/agent/tools/ToolPermissionInterceptor.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/UserTools.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/LoanTools.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/RiskTools.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/AdminTools.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/tools/EnterpriseTools.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/config/LangChain4jConfig.java`

- [ ] **Step 1: 创建ToolPermission注解**

```java
package com.xindai.xindai.modules.agent.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolAllowed {
    String[] portals() default {}; // 允许的端: "user", "admin", "enterprise"
}
```

- [ ] **Step 2: 在各Tool类方法上添加权限注解**

- `UserTools` 所有方法: `@ToolAllowed(portals = {"user", "admin", "enterprise"})`
- `LoanTools` 所有方法: `@ToolAllowed(portals = {"user", "admin", "enterprise"})`
- `RiskTools` 所有方法: `@ToolAllowed(portals = {"user", "admin", "enterprise"})`
- `AdminTools` 所有方法: `@ToolAllowed(portals = {"admin"})`
- `EnterpriseTools` 所有方法: `@ToolAllowed(portals = {"enterprise", "admin"})`

- [ ] **Step 3: 创建ToolPermissionInterceptor**

在每个Tool方法执行前检查当前portal（从ToolContext获取）是否在允许列表中。如果不允许，抛出BusinessException提示无权限。

- [ ] **Step 4: 在LangChain4jConfig中为不同Agent配置不同的Tool集合**

修改Agent创建逻辑，根据portal类型只注入对应权限的Tool。

- [ ] **Step 5: 编译验证**

Run: `cd D:/java_project/xindai_backend && mvn compile -q`

---

## 阶段5: 最终验证

### Task 17: 全量编译与验证

- [ ] **Step 1: 全量编译**

Run: `cd D:/java_project/xindai_backend && mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 检查所有新增文件**

确认所有计划中的文件已创建，所有修改已完成。

- [ ] **Step 3: 检查import引用**

确认所有新类的import路径正确，无遗漏。

---

## 并行执行策略

```
阶段1 (全部可并行):
  ├─ Agent-A: Task 1 (统一Application状态)
  ├─ Agent-B: Task 2 (统一Contract状态)
  ├─ Agent-C: Task 3 (规范化分页)
  ├─ Agent-D: Task 4 (限制Actuator)
  └─ Agent-E: Task 5 (结构化日志)

阶段2 (全部可并行, 但Task 8依赖Task 5的TraceFilter):
  ├─ Agent-F: Task 6 (Flyway)
  ├─ Agent-G: Task 7 (数据脱敏)
  └─ Agent-H: Task 8 (审计日志)

阶段3 (Task 9, 11, 12, 13可并行; Task 10依赖Task 1):
  ├─ Agent-I:  Task 9  (逾期管理)
  ├─ Agent-J:  Task 10 (审批增强) [依赖Task 1]
  ├─ Agent-K:  Task 11 (企业额度)
  ├─ Agent-L:  Task 12 (额度计算)
  └─ Agent-M:  Task 13 (Dashboard性能)

阶段4 (全部可并行):
  ├─ Agent-N: Task 14 (文件上传)
  ├─ Agent-O: Task 15 (消息通知)
  └─ Agent-P: Task 16 (Agent权限)

阶段5 (串行):
  └─ Task 17 (全量验证)
```

**注意**: Task 1和Task 2是核心Bug修复，应最先执行。Task 3/4/5无依赖可并行。Task 8(审计日志)依赖Task 5(TraceFilter)。Task 10(审批增强)依赖Task 1(状态统一)。
