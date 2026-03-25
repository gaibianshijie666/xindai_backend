# Xindai System Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the xindai loan risk control system to production-grade quality across 8 layers: infrastructure, security, event-driven architecture, workflow, business features, notifications, performance, and code quality.

**Architecture:** Bottom-up incremental approach. Each layer builds on the previous one. Layer 1 (infrastructure) provides RabbitMQ, encryption, and multi-env config that all subsequent layers depend on. Layer 2 (security) fixes critical auth issues. Layer 3 (events) decouples modules. Layers 4-5 add new business modules. Layers 6-8 polish the system.

**Tech Stack:** Spring Boot 3.5.11, Java 17, MyBatis-Plus 3.5.5, RabbitMQ, Flowable, Spring Mail, Thymeleaf, OpenPDF, Redis, MySQL, Flyway, LangChain4j, Hutool

**Design Spec:** `docs/superpowers/specs/2026-03-25-system-refactoring-design.md`

---

## File Structure Overview

### New Files to Create

```
src/main/java/com/xindai/xindai/
├── common/
│   ├── event/
│   │   ├── DomainEvent.java
│   │   ├── EventPublisher.java
│   │   ├── impl/RabbitMQEventPublisher.java
│   │   └── impl/LocalEventPublisher.java
│   ├── mybatis/
│   │   ├── CryptoField.java
│   │   └── CryptoInterceptor.java
│   ├── notification/channel/
│   │   ├── NotificationChannel.java
│   │   ├── NotificationMessage.java
│   │   ├── NotificationType.java
│   │   ├── InAppNotificationChannel.java
│   │   ├── EmailNotificationChannel.java
│   │   └── SmsNotificationChannel.java
│   ├── sms/
│   │   ├── SmsService.java
│   │   ├── SendResult.java
│   │   └── MockSmsServiceImpl.java
│   ├── config/
│   │   └── SensitiveConfigValidator.java
│   └── constants/
│       └── QueueConstants.java
├── config/
│   ├── RabbitMQConfig.java
│   ├── FlowableConfig.java
│   └── MailConfig.java
├── modules/
│   ├── workflow/
│   │   ├── service/WorkflowService.java
│   │   ├── service/impl/WorkflowServiceImpl.java
│   │   ├── dto/StartProcessDTO.java
│   │   ├── dto/CompleteTaskDTO.java
│   │   ├── dto/TaskVO.java
│   │   └── controller/WorkflowController.java
│   ├── collection/
│   │   ├── controller/CollectionController.java
│   │   ├── service/CollectionTaskService.java
│   │   ├── service/impl/CollectionTaskServiceImpl.java
│   │   ├── entity/CollectionTask.java
│   │   ├── entity/CollectionRecord.java
│   │   ├── mapper/CollectionTaskMapper.java
│   │   ├── mapper/CollectionRecordMapper.java
│   │   ├── dto/CollectionTaskVO.java
│   │   ├── dto/CollectionRecordVO.java
│   │   ├── dto/CollectionTaskQueryDTO.java
│   │   ├── dto/CreateCollectionRecordDTO.java
│   │   └── enums/CollectionTaskStatus.java
│   ├── loan/
│   │   ├── service/DisbursementService.java
│   │   ├── service/impl/DisbursementServiceImpl.java
│   │   ├── entity/BankAccount.java
│   │   ├── entity/DisbursementRecord.java
│   │   ├── mapper/BankAccountMapper.java
│   │   ├── mapper/DisbursementRecordMapper.java
│   │   ├── dto/BankAccountDTO.java
│   │   ├── dto/BankAccountVO.java
│   │   ├── dto/DisbursementVO.java
│   │   ├── enums/DisbursementStatus.java
│   │   ├── service/ContractPdfService.java
│   │   └── service/impl/ContractPdfServiceImpl.java
│   └── report/
│       ├── service/ReportService.java
│       └── service/impl/ReportServiceImpl.java
├── client/kyc/
│   ├── KycService.java
│   └── MockKycServiceImpl.java
├── listener/
│   ├── RiskAssessmentListener.java
│   ├── ApplicationStatusListener.java
│   ├── DisbursementListener.java
│   ├── ContractListener.java
│   ├── CreditLimitListener.java
│   ├── CollectionListener.java
│   └── NotificationEventListener.java
└── task/
    └── RepaymentReminderTask.java

src/main/resources/
├── application-dev.yml
├── application-prod.yml
├── processes/
│   └── loan-approval.bpmn20.xml
├── templates/email/
│   ├── loan-approved.html
│   ├── loan-rejected.html
│   ├── repayment-reminder.html
│   ├── overdue-notice.html
│   └── contract-generated.html
└── db/migration/
    ├── V8__disbursement_and_kyc.sql
    ├── V9__collection_tables.sql
    ├── V10__contract_template.sql
    └── V12__performance_indexes.sql

src/test/java/com/xindai/xindai/
├── modules/agent/ChatControllerTest.java
├── modules/collection/CollectionTaskServiceTest.java
├── modules/loan/DisbursementServiceTest.java
├── modules/loan/ContractPdfServiceTest.java
├── modules/workflow/WorkflowServiceTest.java
├── common/event/RabbitMQEventPublisherTest.java
├── common/mybatis/CryptoInterceptorTest.java
├── common/sms/MockSmsServiceTest.java
├── security/jwt/JwtAuthenticationFilterTest.java
└── listener/NotificationEventListenerTest.java
```

### Key Files to Modify

```
pom.xml                                    # Add dependencies
src/main/resources/application.yml         # Extract common config
src/main/java/.../XindaiApplication.java   # Possibly add annotations
src/main/java/.../security/config/SecurityConfig.java  # Single JWT filter
src/main/java/.../security/filter/JwtAuthenticationFilter.java  # Merge dual filter
src/main/java/.../security/jwt/JwtUtils.java            # Add userType claim
src/main/java/.../security/filter/EnterpriseJwtAuthenticationFilter.java  # DELETE
src/main/java/.../config/RedisConfig.java    # Add new cache names
src/main/java/.../common/exception/GlobalExceptionHandler.java  # Add handlers
src/main/java/.../common/aspect/OperateLogAspect.java  # Trusted proxy check
src/main/java/.../modules/loan/service/impl/LoanServiceImpl.java  # Events, locks, pagination
src/main/java/.../modules/risk/service/impl/RiskAssessmentServiceImpl.java  # Decouple, Lua lock
src/main/java/.../modules/notification/service/impl/NotificationServiceImpl.java  # Multi-channel
src/main/java/.../modules/admin/service/impl/AdminApplicationServiceImpl.java  # Transaction fix
src/main/java/.../modules/admin/controller/AdminAuthController.java  # @PreAuthorize
src/main/java/.../modules/loan/task/OverdueCheckTask.java  # Publish event
src/main/java/.../modules/risk/controller/RiskController.java  # Move toVO to service
src/main/java/.../modules/enterprise/controller/EnterpriseAuthController.java  # Move VO to service
src/main/java/.../modules/user/controller/UserProfileController.java  # Move VO to service
src/main/java/.../modules/user/dto/RegisterRequest.java  # Validation
src/main/java/.../modules/user/dto/UserLoginDTO.java  # Phone pattern
src/main/java/.../modules/agent/controller/ChatController.java  # Input validation
```

---

## LAYER 1: Infrastructure

### Task 1.1: Split Multi-Environment Configuration

**Files:**
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`
- Create: `src/main/resources/application-prod.yml`
- Test: manual verification (app starts in each profile)

- [ ] **Step 1: Extract common config to application.yml**

Read the current `application.yml` and extract only shared settings:
- Server port (8080)
- MyBatis-Plus configuration (mapper-locations, map-underscore-to-camel-case)
- Flyway config (locations, enabled)
- Logback reference
- Spring application name
- Jackson date format

Remove from application.yml: all datasource, redis, jwt, cors, actuator, third-party, agent, credit-limit configs.

- [ ] **Step 2: Create application-dev.yml**

Move all environment-specific configs here with dev defaults:
- Datasource: localhost MySQL xindai_dev, root/chen
- Redis: localhost:6379, db 0
- JWT: dev secret, 24h expiration
- CORS: localhost:5173,5174
- Actuator: full exposure
- SQL logging: enabled (`log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`)
- Third-party: all disabled
- Agent: enabled with placeholder API key
- Swagger: enabled

- [ ] **Step 3: Create application-prod.yml**

Production config with env var injection (no defaults for secrets):
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:xindai}?useSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
  sql:
    init:
      mode: never

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400}

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

logging:
  level:
    com.xindai.xindai: ${LOG_LEVEL:INFO}
```

- [ ] **Step 4: Verify app starts with dev profile**

Run: `cd D:/java_project/xindai_backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
Expected: Application starts successfully on port 8080.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/application*.yml
git commit -m "refactor: split configuration into multi-environment profiles"
```

---

### Task 1.2: Add RabbitMQ Dependency and Configuration

**Files:**
- Modify: `pom.xml` (add spring-boot-starter-amqp)
- Create: `src/main/java/com/xindai/xindai/config/RabbitMQConfig.java`
- Create: `src/main/java/com/xindai/xindai/common/constants/QueueConstants.java`
- Test: `src/test/java/com/xindai/xindai/common/constants/QueueConstantsTest.java`

- [ ] **Step 1: Add RabbitMQ dependency to pom.xml**

Add after the redis dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

- [ ] **Step 2: Add RabbitMQ config to application-dev.yml**

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
    publisher-confirm-type: correlated
    publisher-returns: true
```

- [ ] **Step 3: Create QueueConstants**

```java
package com.xindai.xindai.common.constants;

public final class QueueConstants {
    public static final String EXCHANGE = "xindai.direct";
    public static final String DLX_EXCHANGE = "xindai.dlx";

    public static final String LOAN_APPLICATION_SUBMITTED = "loan.application.submitted";
    public static final String LOAN_APPLICATION_APPROVED = "loan.application.approved";
    public static final String LOAN_APPLICATION_REJECTED = "loan.application.rejected";
    public static final String LOAN_REPAYMENT_COMPLETED = "loan.repayment.completed";
    public static final String LOAN_OVERDUE_DETECTED = "loan.overdue.detected";
    public static final String RISK_ASSESSMENT_COMPLETED = "risk.assessment.completed";

    private QueueConstants() {}
}
```

- [ ] **Step 4: Create RabbitMQConfig**

```java
package com.xindai.xindai.config;

import com.xindai.xindai.common.constants.QueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange xindaiExchange() {
        return new DirectExchange(QueueConstants.EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(QueueConstants.DLX_EXCHANGE);
    }

    @Bean
    public Queue loanApplicationSubmittedQueue() {
        return QueueBuilder.durable(QueueConstants.LOAN_APPLICATION_SUBMITTED)
                .withArgument("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE).build();
    }

    // ... same pattern for all 6 queues ...

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setExchange(QueueConstants.EXCHANGE);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        return factory;
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application-dev.yml src/main/java/com/xindai/xindai/config/RabbitMQConfig.java src/main/java/com/xindai/xindai/common/constants/QueueConstants.java
git commit -m "feat: add RabbitMQ infrastructure with exchange, queues, and dead letter config"
```

---

### Task 1.3: MyBatis Encryption Interceptor

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/mybatis/CryptoField.java`
- Create: `src/main/java/com/xindai/xindai/common/mybatis/CryptoInterceptor.java`
- Modify: `src/main/java/com/xindai/xindai/modules/user/entity/User.java` (add @CryptoField)
- Test: `src/test/java/com/xindai/xindai/common/mybatis/CryptoInterceptorTest.java`

- [ ] **Step 1: Create @CryptoField annotation**

```java
package com.xindai.xindai.common.mybatis;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CryptoField {
    String algorithm() default "AES";
}
```

- [ ] **Step 2: Create CryptoInterceptor**

Implement MyBatis-Plus InnerInterceptor that:
- On INSERT/UPDATE: intercept parameter object, find @CryptoField annotated fields, encrypt values using Hutool's `SymmetricCrypto` (AES)
- On SELECT: intercept result set, find @CryptoField annotated fields, decrypt values
- Encrypt query parameters for WHERE clauses on encrypted fields
- AES key from env var `ENCRYPTION_KEY`, validated at startup

Use Hutool's `cn.hutool.crypto.symmetric.AES` which is already available via `hutool-all`.

Key implementation points:
- Use `MybatisPlusInterceptor` and implement `InnerInterceptor`
- Intercept at `beforeQuery` for SELECT and `beforePrepare` for INSERT/UPDATE
- Cache reflected field metadata per entity class

- [ ] **Step 3: Register interceptor in existing config**

Add to `com.xindai.xindai.config.RedisConfig` or create a new `MyBatisConfig`:
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(CryptoInterceptor cryptoInterceptor) {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    interceptor.addInnerInterceptor(cryptoInterceptor);
    return interceptor;
}
```

- [ ] **Step 4: Add @CryptoField to User entity**

On `User.java`, annotate:
```java
@CryptoField
private String phone;

@CryptoField
private String idCard;
```

- [ ] **Step 5: Write unit test for CryptoInterceptor**

Test encrypt/decrypt round-trip with sample data.
Run: `./mvnw test -Dtest=CryptoInterceptorTest -pl .`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/xindai/xindai/common/mybatis/ src/main/java/.../user/entity/User.java src/test/...
git commit -m "feat: add MyBatis AES encryption interceptor for sensitive fields"
```

---

### Task 1.4: Flyway Migrations V8-V12

**Files:**
- Create: `src/main/resources/db/migration/V8__disbursement_and_kyc.sql`
- Create: `src/main/resources/db/migration/V9__collection_tables.sql`
- Create: `src/main/resources/db/migration/V10__contract_template.sql`
- Create: `src/main/resources/db/migration/V12__performance_indexes.sql`

- [ ] **Step 1: Create V8 migration**

```sql
-- V8: Disbursement tables + KYC fields
ALTER TABLE user_profile ADD COLUMN identity_status TINYINT DEFAULT 0 COMMENT '0=未认证,1=认证中,2=已认证,3=认证失败';
ALTER TABLE user_profile ADD COLUMN identity_verified_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_name VARCHAR(50) NOT NULL COMMENT '银行名称',
    account_no VARCHAR(30) NOT NULL COMMENT '银行卡号',
    account_name VARCHAR(50) NOT NULL COMMENT '账户名',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认 0=否 1=是',
    status TINYINT DEFAULT 1 COMMENT '0=禁用 1=正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bank_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS disbursement_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    bank_account_id BIGINT NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0=待放款,1=放款中,2=已放款,3=放款失败',
    transaction_no VARCHAR(64) COMMENT '交易流水号',
    completed_at DATETIME,
    failed_reason VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_disbursement_contract (contract_id),
    INDEX idx_disbursement_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Create V9 migration**

```sql
-- V9: Collection tables
CREATE TABLE IF NOT EXISTS collection_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    overdue_amount DECIMAL(12,2) NOT NULL,
    overdue_days INT NOT NULL,
    collector_id BIGINT COMMENT '催收员(管理员)ID',
    status TINYINT DEFAULT 0 COMMENT '0=待分配,1=已分配,2=处理中,3=已完成,4=已关闭',
    priority TINYINT DEFAULT 1 COMMENT '1=低,2=中,3=高',
    deadline DATETIME COMMENT '催收截止日',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_collection_contract (contract_id),
    INDEX idx_collection_status (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS collection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL COMMENT 'phone/sms/visit/legal',
    content TEXT COMMENT '催收内容',
    result VARCHAR(50) COMMENT 'promise_pay/refused/unreachable/other',
    next_follow_up_date DATE COMMENT '下次跟进日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: Create V10 migration**

```sql
-- V10: Contract template table
CREATE TABLE IF NOT EXISTS contract_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'personal/enterprise',
    content TEXT NOT NULL COMMENT '模板内容(HTML)',
    version VARCHAR(20) DEFAULT '1.0',
    status TINYINT DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Create V12 migration (indexes)**

```sql
-- V12: Performance indexes
CREATE INDEX idx_loan_app_user_status ON loan_application(user_id, status);
CREATE INDEX idx_repayment_contract_status ON repayment_plan(contract_id, status);
CREATE INDEX idx_risk_user_created ON risk_assessment(user_id, created_at DESC);
CREATE INDEX idx_notification_user_read ON notification(user_id, is_read, created_at DESC);
```

- [ ] **Step 5: Verify migrations run cleanly**

Run: `./mvnw flyway:migrate -pl .`
Expected: All migrations V1-V12 apply successfully.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V8__*.sql V9__*.sql V10__*.sql V12__*.sql
git commit -m "feat: add Flyway migrations for disbursement, collection, contract template, and indexes"
```

---

## LAYER 2: Security Fixes

### Task 2.1: JWT Filter Refactoring (Merge Dual Filters)

**Files:**
- Modify: `src/main/java/com/xindai/xindai/security/jwt/JwtUtils.java` (add userType)
- Modify: `src/main/java/com/xindai/xindai/security/filter/JwtAuthenticationFilter.java` (handle all types)
- Delete: `src/main/java/com/xindai/xindai/security/filter/EnterpriseJwtAuthenticationFilter.java`
- Modify: `src/main/java/com/xindai/xindai/security/config/SecurityConfig.java` (single filter)
- Modify: all auth services that call `jwtUtils.generateToken()` (add userType param)
- Test: `src/test/java/com/xindai/xindai/security/jwt/JwtAuthenticationFilterTest.java`

- [ ] **Step 1: Add userType support to JwtUtils**

Modify `generateToken` to accept userType:
```java
public String generateToken(Long userId, String phone, String userType, Long enterpriseId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("phone", phone);
    claims.put("userType", userType);    // NEW: USER, ADMIN, ENTERPRISE
    claims.put("enterpriseId", enterpriseId);  // NEW: null for user/admin
    return Jwts.builder()
            .claims(claims)
            .subject(String.valueOf(userId))
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .signWith(getSecretKey())
            .compact();
}
```

Add method:
```java
public String getUserType(String token) {
    return getClaims(token).get("userType", String.class);
}
```

Keep old `generateToken(userId, phone)` as backward-compatible overload that calls the new one with `userType="USER"` and `enterpriseId=null`.

- [ ] **Step 2: Rewrite JwtAuthenticationFilter to handle all user types**

The filter should:
1. Extract token from Authorization header
2. Parse claims including `userType`
3. Based on userType, set appropriate authorities:
   - `USER` → `SimpleGrantedAuthority("ROLE_USER")`
   - `ADMIN` → look up admin_user.role from DB → `ROLE_ADMIN` or `ROLE_SUPER_ADMIN`
   - `ENTERPRISE` → look up enterprise_user.role → `ROLE_ENTERPRISE` + sub-roles
4. Set authentication in SecurityContext

- [ ] **Step 3: Update SecurityConfig**

Remove `EnterpriseJwtAuthenticationFilter` bean and filter registration. Keep only `JwtAuthenticationFilter`.

- [ ] **Step 4: Delete EnterpriseJwtAuthenticationFilter**

- [ ] **Step 5: Update all auth services that generate tokens**

- `UserAuthService` (user login): `generateToken(userId, phone, "USER", null)`
- `AdminAuthService` (admin login): `generateToken(adminId, phone, "ADMIN", null)`
- `EnterpriseAuthService` (enterprise login): `generateToken(userId, username, "ENTERPRISE", enterpriseId)`

- [ ] **Step 6: Write unit test for multi-type JWT parsing**

Test cases:
- USER token → ROLE_USER authority
- ADMIN token → ROLE_ADMIN authority
- ENTERPRISE token → ROLE_ENTERPRISE + ROLE_ENTERPRISE_OPERATOR
- Invalid/expired token → no authentication

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "fix: merge dual JWT filters into single filter with userType support"
```

---

### Task 2.2: Admin Authorization Fix

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/admin/controller/AdminAuthController.java`

- [ ] **Step 1: Add @PreAuthorize to register endpoint**

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
@PostMapping("/register")
public Result<AdminLoginVO> register(@RequestBody @Valid AdminRegisterDTO dto) {
```

- [ ] **Step 2: Verify admin dashboard endpoints have @PreAuthorize**

Check `AdminDashboardController`, `AdminUserController`, `AdminApplicationController`, `AdminBlacklistController` all have `@PreAuthorize("hasRole('ADMIN')")`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/xindai/xindai/modules/admin/controller/AdminAuthController.java
git commit -m "fix: add SUPER_ADMIN authorization check to admin registration"
```

---

### Task 2.3: Transaction Standardization

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminApplicationServiceImpl.java`

- [ ] **Step 1: Find all bare @Transactional annotations**

Search: `@Transactional` without `rollbackFor`
Run: `grep -rn "@Transactional\b" --include="*.java" src/main/ | grep -v rollbackFor`

- [ ] **Step 2: Replace all with rollbackFor = Exception.class**

Change every occurrence of:
```java
@Transactional
```
to:
```java
@Transactional(rollbackFor = Exception.class)
```

- [ ] **Step 3: Run existing tests to verify no breakage**

Run: `./mvnw test -pl .`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: standardize @Transactional with rollbackFor = Exception.class"
```

---

### Task 2.4: Sensitive Config Validator

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/config/SensitiveConfigValidator.java`
- Modify: `src/main/resources/application-prod.yml` (add ENCRYPTION_KEY)

- [ ] **Step 1: Create SensitiveConfigValidator**

```java
package com.xindai.xindai.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SensitiveConfigValidator {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${ENCRYPTION_KEY:}")
    private String encryptionKey;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @PostConstruct
    public void validate() {
        validateNotBlank("jwt.secret", jwtSecret);
        validateNotBlank("ENCRYPTION_KEY", encryptionKey);
        validateNotBlank("spring.datasource.password", dbPassword);
        log.info("Sensitive configuration validation passed");
    }

    private void validateNotBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Required configuration '" + name + "' is not set. " +
                "Please set it via environment variable or application properties.");
        }
    }
}
```

- [ ] **Step 2: Add ENCRYPTION_KEY to prod config**

In `application-prod.yml`:
```yaml
encryption:
  key: ${ENCRYPTION_KEY}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/xindai/xindai/common/config/SensitiveConfigValidator.java src/main/resources/application-prod.yml
git commit -m "feat: add startup validation for required sensitive configuration"
```

---

### Task 2.5: Distributed Lock Enhancement

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/risk/service/impl/RiskAssessmentServiceImpl.java`

- [ ] **Step 1: Add Redis Lua script lock utility**

Create a lock utility method (can be in a `RedisLockUtil` class or inline):
```java
// Lua script for atomic check-and-delete
private static final String UNLOCK_SCRIPT =
    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
    "    return redis.call('del', KEYS[1]) " +
    "else " +
    "    return 0 " +
    "end";

public boolean releaseLock(String lockKey, String lockValue) {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    Long result = stringRedisTemplate.execute(script, List.of(lockKey), lockValue);
    return Long.valueOf(1L).equals(result);
}
```

- [ ] **Step 2: Add lock to LoanServiceImpl.getCreditLimit()**

```java
String lockKey = "credit_limit:lock:" + userId;
String lockValue = UUID.randomUUID().toString();
try {
    Boolean acquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);
    if (Boolean.FALSE.equals(acquired)) {
        throw new BusinessException(ErrorCode.SYSTEM_BUSY);
    }
    // ... existing logic ...
} finally {
    releaseLock(lockKey, lockValue);
}
```

- [ ] **Step 3: Apply same pattern to RiskAssessmentServiceImpl**

Replace the simple `delete` lock release with Lua script.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: use Lua script for atomic distributed lock release"
```

---

### Task 2.6: Exception Handler and Security Improvements

**Files:**
- Modify: `src/main/java/com/xindai/xindai/common/exception/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/xindai/xindai/common/aspect/OperateLogAspect.java`

- [ ] **Step 1: Add AccessDeniedException handler**

```java
@ExceptionHandler(AccessDeniedException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
public Result<Void> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
    String traceId = MDC.get("traceId");
    log.warn("Access denied: {} - traceId: {}", e.getMessage(), traceId);
    return Result.error(403, "访问被拒绝：" + e.getMessage());
}
```

- [ ] **Step 2: Add HttpMessageNotReadableException handler**

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
    String traceId = MDC.get("traceId");
    log.warn("Malformed request body - traceId: {}", traceId);
    return Result.error(400, "请求格式错误");
}
```

- [ ] **Step 3: Add trusted proxy check to OperateLogAspect**

Reference the `isTrustedProxy()` method from `RateLimitInterceptor` and apply the same check before trusting `X-Forwarded-For`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xindai/xindai/common/exception/GlobalExceptionHandler.java src/main/java/com/xindai/xindai/common/aspect/OperateLogAspect.java
git commit -m "fix: add exception handlers and trusted proxy check for security"
```

---

## LAYER 3: Event-Driven Refactoring

### Task 3.1: Define Domain Events

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/event/DomainEvent.java`
- Create: `src/main/java/com/xindai/xindai/common/event/LoanApplicationSubmittedEvent.java`
- Create: `src/main/java/com/xindai/xindai/common/event/LoanApplicationApprovedEvent.java`
- Create: `src/main/java/com/xindai/xindai/common/event/LoanApplicationRejectedEvent.java`
- Create: `src/main/java/com/xindai/xindai/common/event/RiskAssessmentCompletedEvent.java`
- Create: `src/main/java/com/xindai/xindai/common/event/RepaymentCompletedEvent.java`
- Create: `src/main/java/com/xindai/xindai/common/event/LoanOverdueDetectedEvent.java`
- Create: `src/main/java/com/xindai/xindai/common/event/DisbursementCompletedEvent.java`

- [ ] **Step 1: Create DomainEvent base class**

```java
package com.xindai.xindai.common.event;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public abstract class DomainEvent {
    private String eventId;
    private LocalDateTime occurredAt;
    private String traceId;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.traceId = org.slf4j.MDC.get("traceId");
    }
}
```

- [ ] **Step 2: Create all 7 concrete event classes**

Each extends `DomainEvent` with specific fields as defined in the design spec. Use Lombok `@Data` and `@AllArgsConstructor`.

Example:
```java
package com.xindai.xindai.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class LoanApplicationSubmittedEvent extends DomainEvent {
    private Long applicationId;
    private Long userId;
    private BigDecimal amount;
    private Integer term;
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/xindai/xindai/common/event/
git commit -m "feat: define domain events for event-driven architecture"
```

---

### Task 3.2: Event Publisher

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/event/EventPublisher.java`
- Create: `src/main/java/com/xindai/xindai/common/event/impl/RabbitMQEventPublisher.java`
- Create: `src/main/java/com/xindai/xindai/common/event/impl/LocalEventPublisher.java`
- Test: `src/test/java/com/xindai/xindai/common/event/RabbitMQEventPublisherTest.java`

- [ ] **Step 1: Create EventPublisher interface**

```java
package com.xindai.xindai.common.event;

public interface EventPublisher {
    void publish(DomainEvent event);
}
```

- [ ] **Step 2: Create RabbitMQEventPublisher**

```java
package com.xindai.xindai.common.event.impl;

import com.xindai.xindai.common.event.DomainEvent;
import com.xindai.xindai.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher.type", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        String routingKey = resolveRoutingKey(event);
        log.info("Publishing event: {} to routingKey: {}", event.getClass().getSimpleName(), routingKey);
        rabbitTemplate.convertAndSend(routingKey, event);
    }

    private String resolveRoutingKey(DomainEvent event) {
        return switch (event.getClass().getSimpleName()) {
            case "LoanApplicationSubmittedEvent" -> "loan.application.submitted";
            case "LoanApplicationApprovedEvent" -> "loan.application.approved";
            case "LoanApplicationRejectedEvent" -> "loan.application.rejected";
            case "RepaymentCompletedEvent" -> "loan.repayment.completed";
            case "LoanOverdueDetectedEvent" -> "loan.overdue.detected";
            case "RiskAssessmentCompletedEvent" -> "risk.assessment.completed";
            case "DisbursementCompletedEvent" -> "loan.application.approved";
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getClass());
        };
    }
}
```

- [ ] **Step 3: Create LocalEventPublisher as fallback**

Uses `ApplicationEventPublisher` for environments without RabbitMQ.

- [ ] **Step 4: Write test**

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/xindai/xindai/common/event/
git commit -m "feat: implement RabbitMQ event publisher with local fallback"
```

---

### Task 3.3: Event Listeners

**Files:**
- Create: `src/main/java/com/xindai/xindai/listener/RiskAssessmentListener.java`
- Create: `src/main/java/com/xindai/xindai/listener/ApplicationStatusListener.java`
- Create: `src/main/java/com/xindai/xindai/listener/DisbursementListener.java`
- Create: `src/main/java/com/xindai/xindai/listener/ContractListener.java`
- Create: `src/main/java/com/xindai/xindai/listener/CreditLimitListener.java`
- Create: `src/main/java/com/xindai/xindai/listener/CollectionListener.java`
- Create: `src/main/java/com/xindai/xindai/listener/NotificationEventListener.java`

- [ ] **Step 1: Create RiskAssessmentListener**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskAssessmentListener {

    private final RiskAssessmentService riskAssessmentService;

    @RabbitListener(queues = QueueConstants.LOAN_APPLICATION_SUBMITTED)
    public void onApplicationSubmitted(LoanApplicationSubmittedEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            riskAssessmentService.assess(event.getUserId(), event.getApplicationId(), 2);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process application submitted event: {}", event.getEventId(), e);
            channel.basicNack(tag, false, true);
        }
    }
}
```

- [ ] **Step 2: Create remaining 6 listeners following same pattern**

Each listener:
- Uses `@RabbitListener` with the appropriate queue
- Manually ACK/NACK
- Delegates to the appropriate service
- Logs errors on failure

Key behaviors:
- `ApplicationStatusListener`: reads risk decision, updates loan application status
- `CreditLimitListener`: on repayment, restores available credit limit
- `CollectionListener`: creates collection_task from overdue event
- `NotificationEventListener`: maps events to notification types and sends multi-channel notifications

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/xindai/xindai/listener/
git commit -m "feat: implement event listeners for async business processing"
```

---

### Task 3.4: Refactor Services to Use Events

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/task/OverdueCheckTask.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminApplicationServiceImpl.java`

- [ ] **Step 1: Refactor LoanServiceImpl.apply()**

Replace direct `riskAssessmentService.assess()` call with event publishing:
```java
// Before:
RiskAssessment assessment = riskAssessmentService.assess(userId, application.getId(), 2);
// ... process assessment result synchronously

// After:
eventPublisher.publish(new LoanApplicationSubmittedEvent(
    application.getId(), userId, dto.getAmount(), dto.getTerm()
));
```

Note: The `apply()` method should still return the application VO immediately with PENDING status. The status update happens asynchronously via the listener chain.

- [ ] **Step 2: Refactor LoanServiceImpl.repay()**

Replace direct credit limit manipulation with event publishing:
```java
eventPublisher.publish(new RepaymentCompletedEvent(
    contractId, userId, plan.getPeriod(), dto.getAmount()
));
```

- [ ] **Step 3: Refactor OverdueCheckTask**

Replace direct collection task creation with event:
```java
eventPublisher.publish(new LoanOverdueDetectedEvent(
    contract.getId(), contract.getUserId(), overdueDays, overdueAmount
));
```

- [ ] **Step 4: Refactor AdminApplicationServiceImpl.reviewApplication()**

When admin approves, publish event:
```java
eventPublisher.publish(new LoanApplicationApprovedEvent(
    applicationId, application.getUserId(), application.getAmount(),
    approvedAmount, reviewDTO.getReviewNote()
));
```

- [ ] **Step 5: Run all tests**

Run: `./mvnw test -pl .`
Expected: All existing tests pass (event publisher can be mocked).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: replace synchronous calls with event-driven communication"
```

---

### Task 3.5: Module Decoupling (Risk → User)

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/risk/service/impl/RiskAssessmentServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/risk/feature/FeatureAggregationService.java`
- Modify: `src/main/java/com/xindai/xindai/modules/risk/feature/extractor/*.java`

- [ ] **Step 1: Add getUser methods to UserService interface**

Add to `UserService` or `UserProfileService`:
```java
User getUserById(Long userId);
UserProfile getUserProfileByUserId(Long userId);
```

- [ ] **Step 2: Replace direct mapper calls in risk module**

In `RiskAssessmentServiceImpl`, replace:
```java
// Before:
userMapper.selectById(userId)
userProfileMapper.selectOne(wrapper)
// After:
userService.getUserById(userId)
userProfileService.getUserProfileByUserId(userId)
```

Same for `FeatureAggregationService` and all feature extractors.

- [ ] **Step 3: Remove unused mapper imports from risk module**

- [ ] **Step 4: Run tests**

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: decouple risk module from user module mappers"
```

---

## LAYER 4: Flowable Workflow + Disbursement

### Task 4.1: Add Flowable Dependency and Config

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/xindai/xindai/config/FlowableConfig.java`
- Create: `src/main/resources/processes/loan-approval.bpmn20.xml`

- [ ] **Step 1: Add Flowable dependency**

```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-process</artifactId>
    <version>7.1.0</version>
</dependency>
```

- [ ] **Step 2: Create FlowableConfig**

Minimal config: disable async executor for simplicity, set process engine config.

- [ ] **Step 3: Create BPMN process definition**

Create `loan-approval.bpmn20.xml` with the flow defined in the design spec:
- Start → Service Task (auto risk assessment) → Exclusive Gateway
- Low risk → End (auto approve)
- Medium risk → User Task (manual review) → Exclusive Gateway
- High risk → End (auto reject)

The "auto risk assessment" service task should publish a `RiskAssessmentCompletedEvent`. The user task is assigned to the `ADMIN` role group.

- [ ] **Step 4: Verify Flowable deploys process**

Start the app and check Flowable tables are created and process is deployed.
Run: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/xindai/xindai/config/FlowableConfig.java src/main/resources/processes/
git commit -m "feat: add Flowable workflow engine with loan approval process"
```

---

### Task 4.2: Workflow Service and Controller

**Files:**
- Create: `src/main/java/com/xindai/xindai/modules/workflow/service/WorkflowService.java`
- Create: `src/main/java/com/xindai/xindai/modules/workflow/service/impl/WorkflowServiceImpl.java`
- Create: `src/main/java/com/xindai/xindai/modules/workflow/dto/StartProcessDTO.java`
- Create: `src/main/java/com/xindai/xindai/modules/workflow/dto/CompleteTaskDTO.java`
- Create: `src/main/java/com/xindai/xindai/modules/workflow/dto/TaskVO.java`
- Create: `src/main/java/com/xindai/xindai/modules/workflow/controller/WorkflowController.java`
- Test: `src/test/java/com/xindai/xindai/modules/workflow/WorkflowServiceTest.java`

- [ ] **Step 1: Create DTOs**

`StartProcessDTO`: processDefinitionKey, businessKey, variables (Map)
`CompleteTaskDTO`: taskId, variables (Map), action (approve/reject/return)
`TaskVO`: taskId, taskName, assignee, createTime, processInstanceId, variables

- [ ] **Step 2: Create WorkflowService interface**

```java
public interface WorkflowService {
    String startProcess(String processKey, String businessKey, Map<String, Object> variables);
    List<TaskVO> getPendingTasks(String assignee);
    List<TaskVO> getPendingTasksByGroup(String group);
    void completeTask(String taskId, Map<String, Object> variables);
    TaskVO getTaskDetail(String taskId);
    String getProcessStatus(String processInstanceId);
}
```

- [ ] **Step 3: Implement WorkflowServiceImpl**

Inject `RuntimeService`, `TaskService`, `HistoryService`. Implement all methods.

- [ ] **Step 4: Create WorkflowController**

```java
@RestController
@RequestMapping("/api/v1/admin/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流管理")
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<TaskVO>> getPendingTasks() { ... }

    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> completeTask(@PathVariable String taskId, @RequestBody CompleteTaskDTO dto) { ... }
}
```

- [ ] **Step 5: Write test**

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/xindai/xindai/modules/workflow/
git commit -m "feat: add workflow service and controller for Flowable process management"
```

---

### Task 4.3: Disbursement Service

**Files:**
- Create: `src/main/java/com/xindai/xindai/modules/loan/service/DisbursementService.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/service/impl/DisbursementServiceImpl.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/entity/BankAccount.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/entity/DisbursementRecord.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/mapper/BankAccountMapper.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/mapper/DisbursementRecordMapper.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/dto/BankAccountDTO.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/dto/BankAccountVO.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/dto/DisbursementVO.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/enums/DisbursementStatus.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/controller/BankAccountController.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/controller/DisbursementController.java`
- Test: `src/test/java/com/xindai/xindai/modules/loan/DisbursementServiceTest.java`

- [ ] **Step 1: Create entities, enums, mappers**

Follow existing patterns (e.g., `LoanContract`, `RepaymentPlan`).

`DisbursementStatus` enum: PENDING(0), PROCESSING(1), COMPLETED(2), FAILED(3)

- [ ] **Step 2: Create DTOs**

`BankAccountDTO`: bankName, accountNo, accountName, isDefault
`DisbursementVO`: id, contractId, amount, status, transactionNo, completedAt

- [ ] **Step 3: Implement DisbursementService**

```java
public interface DisbursementService {
    BankAccountVO addBankAccount(Long userId, BankAccountDTO dto);
    List<BankAccountVO> getBankAccounts(Long userId);
    DisbursementVO initiateDisbursement(Long contractId, Long bankAccountId);
    DisbursementVO confirmDisbursement(Long disbursementId);
    DisbursementVO getDisbursementStatus(Long disbursementId);
}
```

Key logic in `initiateDisbursement`:
1. Validate contract is APPROVED
2. Create disbursement_record with PENDING status
3. Transition contract status to DISBURSED
4. Generate repayment plans if not already generated
5. Publish `DisbursementCompletedEvent`

- [ ] **Step 4: Create controllers**

Bank account CRUD endpoints under `/api/v1/loan/bank-accounts`.
Disbursement endpoints under `/api/v1/admin/disbursements` (admin confirms disbursement).

- [ ] **Step 5: Wire DisbursementListener**

The `DisbursementListener` from Task 3.3 should call `DisbursementService.initiateDisbursement()` when it receives `LoanApplicationApprovedEvent`.

- [ ] **Step 6: Write test**

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/xindai/xindai/modules/loan/ src/main/java/com/xindai/xindai/listener/DisbursementListener.java
git commit -m "feat: add disbursement service with bank account management"
```

---

## LAYER 5: Business Feature Completion

### Task 5.1: Collection Module

**Files:**
- Create: all files under `modules/collection/` (controller, service, entity, mapper, dto, enums)
- Test: `src/test/java/com/xindai/xindai/modules/collection/CollectionTaskServiceTest.java`

- [ ] **Step 1: Create enums**

`CollectionTaskStatus`: PENDING(0), ASSIGNED(1), IN_PROGRESS(2), COMPLETED(3), CLOSED(4)

- [ ] **Step 2: Create entities and mappers**

Follow existing patterns. Both entities use `@TableName` with MyBatis-Plus.

- [ ] **Step 3: Create DTOs**

`CollectionTaskVO`, `CollectionRecordVO`, `CollectionTaskQueryDTO`, `CreateCollectionRecordDTO`

- [ ] **Step 4: Implement CollectionTaskService**

```java
public interface CollectionTaskService {
    Page<CollectionTaskVO> getTaskList(CollectionTaskQueryDTO query);
    CollectionTaskVO getTaskDetail(Long taskId);
    void assignTask(Long taskId, Long collectorId);
    void startTask(Long taskId);
    CollectionRecordVO addRecord(Long taskId, CreateCollectionRecordDTO dto);
    void completeTask(Long taskId);
    void closeTask(Long taskId);
    Page<CollectionRecordVO> getRecords(Long taskId);
}
```

- [ ] **Step 5: Create CollectionController**

Admin endpoints under `/api/v1/admin/collection/`.

- [ ] **Step 6: Wire CollectionListener**

`CollectionListener.onOverdueDetected()` creates a `CollectionTask` from the event.

- [ ] **Step 7: Write test**

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/xindai/xindai/modules/collection/
git commit -m "feat: add collection module with task management and records"
```

---

### Task 5.2: KYC Enhancement

**Files:**
- Create: `src/main/java/com/xindai/xindai/client/kyc/KycService.java`
- Create: `src/main/java/com/xindai/xindai/client/kyc/MockKycServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/user/service/impl/UserProfileServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java` (add KYC check)

- [ ] **Step 1: Create KycService interface**

```java
public interface KycService {
    KycResult verify(String realName, String idCard);
}
```

- [ ] **Step 2: Create MockKycServiceImpl**

Mock implementation: validate name length > 0 and idCard matches 18-digit pattern. Randomly simulate success/failure (90% success rate). Log the verification attempt.

- [ ] **Step 3: Update UserProfileServiceImpl.verifyIdentity()**

Call `kycService.verify()` and set `identityStatus`:
- Before verify: `PENDING(1)`
- Success: `VERIFIED(2)`, set `identityVerifiedAt`
- Failure: `FAILED(3)`

- [ ] **Step 4: Add KYC check to LoanServiceImpl.apply()**

At the start of `apply()`, check user's `identityStatus`:
```java
UserProfile profile = userProfileMapper.selectOne(...);
if (profile.getIdentityStatus() == null || profile.getIdentityStatus() != 2) {
    throw new BusinessException(ErrorCode.IDENTITY_NOT_VERIFIED);
}
```

Add `IDENTITY_NOT_VERIFIED` to ErrorCode enum.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/xindai/xindai/client/kyc/ src/main/java/com/xindai/xindai/modules/user/ src/main/java/com/xindai/xindai/modules/loan/
git commit -m "feat: add KYC verification with mock implementation and loan apply gate"
```

---

### Task 5.3: Electronic Contract (PDF Generation)

**Files:**
- Modify: `pom.xml` (add OpenPDF dependency)
- Create: `src/main/java/com/xindai/xindai/modules/loan/service/ContractPdfService.java`
- Create: `src/main/java/com/xindai/xindai/modules/loan/service/impl/ContractPdfServiceImpl.java`
- Test: `src/test/java/com/xindai/xindai/modules/loan/ContractPdfServiceTest.java`

- [ ] **Step 1: Add OpenPDF dependency**

```xml
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.35</version>
</dependency>
```

- [ ] **Step 2: Implement ContractPdfService**

```java
public interface ContractPdfService {
    byte[] generateContract(LoanContract contract, User user, List<RepaymentPlan> plans);
}
```

Implementation generates a PDF with:
- Header: company name + contract title
- Section 1: Party information (borrower name, ID, lender info)
- Section 2: Loan details (amount, rate, term, total repayment)
- Section 3: Repayment schedule table (period, due date, principal, interest, total)
- Section 4: Terms and conditions (standard legal text)
- Footer: Both party signature areas (mock: pre-stored seal image)

Use `com.lowagie.text.Document`, `PdfPTable`, etc.

- [ ] **Step 3: Store seal image**

Place a mock seal/stamp image at `src/main/resources/static/seal.png`.

- [ ] **Step 4: Wire into ContractListener**

When `LoanApplicationApprovedEvent` is received:
1. Generate contract PDF
2. Upload to file service
3. Save document_url to `loan_contract`
4. Send notification

- [ ] **Step 5: Add contract download endpoint**

```java
@GetMapping("/contracts/{id}/pdf")
public void downloadContract(@PathVariable Long id, HttpServletResponse response) {
    // Load PDF, set Content-Disposition header, write to response
}
```

- [ ] **Step 6: Write test (verify PDF generation doesn't throw)**

- [ ] **Step 7: Commit**

```bash
git add pom.xml src/main/java/com/xindai/xindai/modules/loan/service/ContractPdfService.java src/main/java/com/xindai/xindai/modules/loan/service/impl/ContractPdfServiceImpl.java src/main/resources/static/seal.png
git commit -m "feat: add PDF contract generation with mock seal"
```

---

### Task 5.4: Report Export

**Files:**
- Create: `src/main/java/com/xindai/xindai/modules/report/service/ReportService.java`
- Create: `src/main/java/com/xindai/xindai/modules/report/service/impl/ReportServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/controller/AdminReportController.java` (new controller)

- [ ] **Step 1: Create ReportService**

```java
public interface ReportService {
    void exportLoanLedger(HttpServletResponse response, ApplicationQueryDTO query);
    void exportOverdueReport(HttpServletResponse response);
    void exportRiskStats(HttpServletResponse response, String range);
    void exportCollectionPerformance(HttpServletResponse response);
}
```

- [ ] **Step 2: Implement using EasyExcel**

Follow the pattern from `EnterpriseCustomerServiceImpl.export()`. Each export method:
1. Queries data from respective services/mappers
2. Maps to Excel VO classes
3. Writes using `EasyExcel.write(response.getOutputStream(), VO.class).sheet().doWrite(data)`

- [ ] **Step 3: Create AdminReportController**

```java
@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController { ... }
```

Endpoints: `/loans/export`, `/overdue/export`, `/risk/export`, `/collection/export`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xindai/xindai/modules/report/
git commit -m "feat: add report export service with Excel output"
```

---

## LAYER 6: Notification Enhancement

### Task 6.1: Multi-Channel Notification Architecture

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/notification/channel/NotificationChannel.java`
- Create: `src/main/java/com/xindai/xindai/common/notification/channel/NotificationMessage.java`
- Create: `src/main/java/com/xindai/xindai/common/notification/channel/NotificationType.java`
- Create: `src/main/java/com/xindai/xindai/common/notification/channel/InAppNotificationChannel.java`
- Create: `src/main/java/com/xindai/xindai/common/notification/channel/EmailNotificationChannel.java`
- Create: `src/main/java/com/xindai/xindai/common/notification/channel/SmsNotificationChannel.java`

- [ ] **Step 1: Create NotificationType enum**

```java
public enum NotificationType {
    APPLICATION_SUBMITTED, RISK_COMPLETED,
    LOAN_APPROVED, LOAN_REJECTED,
    DISBURSEMENT_COMPLETED,
    REPAYMENT_REMINDER, REPAYMENT_COMPLETED,
    OVERDUE_NOTICE, COLLECTION_NOTICE,
    CONTRACT_GENERATED
}
```

- [ ] **Step 2: Create NotificationMessage**

```java
@Data
@Builder
public class NotificationMessage {
    private Long userId;
    private String userType;
    private String to;           // phone or email
    private String subject;
    private String content;
    private NotificationType type;
    private String templateCode;
    private Map<String, String> params;
    private String relatedId;
}
```

- [ ] **Step 3: Create NotificationChannel interface**

```java
public interface NotificationChannel {
    boolean supports(NotificationType type);
    void send(NotificationMessage message);
}
```

- [ ] **Step 4: Implement InAppNotificationChannel**

Wraps existing `NotificationServiceImpl.send()` logic. Supports all types.

- [ ] **Step 5: Implement EmailNotificationChannel** (skeleton, fill in Task 6.2)

- [ ] **Step 6: Implement SmsNotificationChannel** (skeleton, fill in Task 6.3)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/xindai/xindai/common/notification/channel/
git commit -m "feat: add multi-channel notification architecture"
```

---

### Task 6.2: Spring Mail Integration

**Files:**
- Modify: `pom.xml` (add spring-boot-starter-mail, spring-boot-starter-thymeleaf)
- Create: `src/main/java/com/xindai/xindai/config/MailConfig.java`
- Create: `src/main/resources/templates/email/loan-approved.html`
- Create: `src/main/resources/templates/email/loan-rejected.html`
- Create: `src/main/resources/templates/email/repayment-reminder.html`
- Create: `src/main/resources/templates/email/overdue-notice.html`
- Create: `src/main/resources/templates/email/contract-generated.html`
- Modify: `src/main/java/com/xindai/xindai/common/notification/channel/EmailNotificationChannel.java`

- [ ] **Step 1: Add dependencies**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

- [ ] **Step 2: Add mail config to application-dev.yml**

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.qq.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

- [ ] **Step 3: Create email templates**

5 HTML templates in `resources/templates/email/`. Each template uses Thymeleaf syntax:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<h1>贷款审批通过</h1>
<p>尊敬的 <span th:text="${userName}"></span>，您的贷款申请已通过审批。</p>
<p>贷款金额：<span th:text="${amount}"></span> 元</p>
<p>请登录系统查看详情。</p>
</body>
</html>
```

- [ ] **Step 4: Implement EmailNotificationChannel**

Inject `JavaMailSender` and `TemplateEngine`. For supported types (LOAN_APPROVED, LOAN_REJECTED, REPAYMENT_REMINDER, OVERDUE_NOTICE, CONTRACT_GENERATED):
1. Resolve template based on type
2. Render with Thymeleaf using params
3. Send via JavaMailSender

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/templates/ src/main/java/com/xindai/xindai/config/MailConfig.java src/main/java/com/xindai/xindai/common/notification/channel/EmailNotificationChannel.java
git commit -m "feat: add Spring Mail integration with Thymeleaf email templates"
```

---

### Task 6.3: SMS Mock Service

**Files:**
- Create: `src/main/java/com/xindai/xindai/common/sms/SmsService.java`
- Create: `src/main/java/com/xindai/xindai/common/sms/SendResult.java`
- Create: `src/main/java/com/xindai/xindai/common/sms/MockSmsServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/common/notification/channel/SmsNotificationChannel.java`
- Test: `src/test/java/com/xindai/xindai/common/sms/MockSmsServiceTest.java`

- [ ] **Step 1: Create SmsService interface**

```java
public interface SmsService {
    SendResult send(String phone, String templateCode, Map<String, String> params);
}
```

- [ ] **Step 2: Create SendResult**

```java
@Data
@Builder
public class SendResult {
    private boolean success;
    private String messageId;
    private String message;
}
```

- [ ] **Step 3: Create MockSmsServiceImpl**

```java
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.enabled", havingValue = "false", matchIfMissing = true)
public class MockSmsServiceImpl implements SmsService {
    @Override
    public SendResult send(String phone, String templateCode, Map<String, String> params) {
        log.info("[MOCK SMS] To: {}, Template: {}, Params: {}", phone, templateCode, params);
        return SendResult.builder()
                .success(true)
                .messageId("MOCK-" + UUID.randomUUID())
                .message("SMS sent (mock)")
                .build();
    }
}
```

- [ ] **Step 4: Implement SmsNotificationChannel**

Supports: LOAN_APPROVED, REPAYMENT_REMINDER, OVERDUE_NOTICE, COLLECTION_NOTICE, DISBURSEMENT_COMPLETED

- [ ] **Step 5: Write test**

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/xindai/xindai/common/sms/
git commit -m "feat: add SMS service with mock implementation"
```

---

### Task 6.4: Refactor NotificationServiceImpl to Use Channels

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/notification/service/impl/NotificationServiceImpl.java`
- Create: `src/main/java/com/xindai/xindai/listener/NotificationEventListener.java`

- [ ] **Step 1: Inject all channels into NotificationServiceImpl**

```java
private final List<NotificationChannel> channels;

public void sendMultiChannel(NotificationMessage message) {
    for (NotificationChannel channel : channels) {
        if (channel.supports(message.getType())) {
            try {
                channel.send(message);
            } catch (Exception e) {
                log.error("Failed to send notification via channel: {}", channel.getClass().getSimpleName(), e);
            }
        }
    }
}
```

- [ ] **Step 2: Create NotificationEventListener**

Listen to all business events and map them to `NotificationMessage`:
```java
@RabbitListener(queues = QueueConstants.LOAN_APPLICATION_APPROVED)
public void onApproved(LoanApplicationApprovedEvent event, ...) {
    User user = userService.getById(event.getUserId());
    NotificationMessage message = NotificationMessage.builder()
            .userId(user.getId())
            .userType("USER")
            .to(user.getPhone())
            .userName(user.getRealName())
            .amount(event.getApprovedAmount().toString())
            .type(NotificationType.LOAN_APPROVED)
            .relatedId(event.getApplicationId().toString())
            .build();
    notificationService.sendMultiChannel(message);
    channel.basicAck(tag, false);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/xindai/xindai/modules/notification/ src/main/java/com/xindai/xindai/listener/NotificationEventListener.java
git commit -m "feat: refactor notification to multi-channel with event-driven delivery"
```

---

### Task 6.5: Scheduled Repayment Reminder

**Files:**
- Create: `src/main/java/com/xindai/xindai/task/RepaymentReminderTask.java`

- [ ] **Step 1: Create RepaymentReminderTask**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentReminderTask {

    private final RepaymentPlanMapper repaymentPlanMapper;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendRepaymentReminders() {
        LocalDate today = LocalDate.now();
        LocalDate reminderDate = today.plusDays(3);

        List<RepaymentPlan> plans = repaymentPlanMapper.selectList(
            new LambdaQueryWrapper<RepaymentPlan>()
                .eq(RepaymentPlan::getStatus, 0)
                .le(RepaymentPlan::getDueDate, reminderDate)
                .ge(RepaymentPlan::getDueDate, today)
        );

        // Deduplicate: one reminder per user per contract per day
        Map<String, RepaymentPlan> deduped = new LinkedHashMap<>();
        for (RepaymentPlan plan : plans) {
            String key = plan.getContractId() + ":" + plan.getUserId();
            if (!deduped.containsKey(key)) {
                deduped.put(key, plan);
            }
        }

        for (RepaymentPlan plan : deduped.values()) {
            // Send multi-channel reminder
            notificationService.sendMultiChannel(...);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/xindai/xindai/task/RepaymentReminderTask.java
git commit -m "feat: add scheduled repayment reminder task"
```

---

## LAYER 7: Cache & Performance

### Task 7.1: Redis Cache Completion

**Files:**
- Modify: `src/main/java/com/xindai/xindai/config/RedisConfig.java` (add new cache names)
- Modify: `src/main/java/com/xindai/xindai/modules/user/service/impl/UserProfileServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/risk/service/impl/BlacklistServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminDashboardServiceImpl.java`

- [ ] **Step 1: Add new cache names to RedisConfig**

Add to cache configuration:
```java
map.put("riskAssessment", 1800);    // 30 min
map.put("dashboardStats", 300);      // 5 min
map.put("systemConfig", 1800);       // 30 min
```

- [ ] **Step 2: Add @Cacheable to UserProfileService**

```java
@Cacheable(value = "userProfile", key = "#userId")
public UserProfileVO getProfileDetail(Long userId) { ... }

@CacheEvict(value = "userProfile", key = "#userId")
public UserProfileVO updateProfileDetail(Long userId, UserProfileUpdateDTO dto) { ... }
```

- [ ] **Step 3: Add @Cacheable to BlacklistService**

```java
@Cacheable(value = "blacklist", key = "#type")
public List<Blacklist> getBlacklist(Integer type) { ... }

@CacheEvict(value = "blacklist", key = "#type")
public void addToBlacklist(Integer type, String value, String reason) { ... }
```

- [ ] **Step 4: Add @Cacheable to AdminDashboardService**

```java
@Cacheable(value = "dashboardStats", key = "'overview'")
public DashboardOverviewVO getOverview() { ... }
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "perf: add Redis caching to userProfile, blacklist, dashboard, and risk assessment"
```

---

### Task 7.2: N+1 Query Fix

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`

- [ ] **Step 1: Fix getPendingRepayment() batch query**

Replace N+1 loop with batch query:
```java
List<RepaymentPlan> plans = repaymentPlanMapper.selectList(wrapper);
if (plans.isEmpty()) return Collections.emptyList();

// Batch fetch contracts
List<Long> contractIds = plans.stream()
    .map(RepaymentPlan::getContractId)
    .distinct()
    .toList();
Map<Long, LoanContract> contractMap = contractMapper.selectBatchIds(contractIds)
    .stream()
    .collect(Collectors.toMap(LoanContract::getId, c -> c));

return plans.stream().map(plan -> {
    RepaymentPlanVO vo = convertToVO(plan);
    LoanContract contract = contractMap.get(plan.getContractId());
    if (contract != null) vo.setContractNo(contract.getContractNo());
    return vo;
}).toList();
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java
git commit -m "perf: fix N+1 query in pending repayment listing"
```

---

### Task 7.3: Pagination for List Endpoints

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/LoanService.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/loan/controller/LoanController.java`
- Modify: `src/main/java/com/xindai/xindai/modules/risk/service/RiskAssessmentService.java`
- Modify: `src/main/java/com/xindai/xindai/modules/risk/service/impl/RiskAssessmentServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/modules/risk/controller/RiskController.java`

- [ ] **Step 1: Add pagination to LoanService interface**

Change return types from `List<VO>` to `Page<VO>` and add page/size params:
```java
Page<LoanApplicationVO> getApplications(Long userId, int page, int size);
Page<LoanContractVO> getContracts(Long userId, int page, int size);
Page<RepaymentPlanVO> getAllRepaymentPlans(Long userId, int page, int size);
```

- [ ] **Step 2: Implement pagination in LoanServiceImpl**

Use MyBatis-Plus `Page<LoanApplication>` and `selectPage()`.

- [ ] **Step 3: Update LoanController endpoints**

Accept `@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size`.

- [ ] **Step 4: Fix RiskAssessmentServiceImpl hardcoded LIMIT 20**

Replace with proper pagination:
```java
Page<RiskAssessment> page = new Page<>(pageNum, pageSize);
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add pagination to all list endpoints"
```

---

## LAYER 8: Code Quality & Tests

### Task 8.1: Fix Layering Violations

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/risk/controller/RiskController.java`
- Modify: `src/main/java/com/xindai/xindai/modules/enterprise/controller/EnterpriseAuthController.java`
- Modify: `src/main/java/com/xindai/xindai/modules/user/controller/UserProfileController.java`
- Modify: corresponding service interfaces and implementations

- [ ] **Step 1: Move RiskController.toVO() to RiskAssessmentService**

Add `RiskAssessmentVO toVO(RiskAssessment assessment)` to service. Controller calls service method.

- [ ] **Step 2: Move EnterpriseAuthController.profile() VO construction to EnterpriseAuthService**

Add `EnterpriseUserVO getProfile(Long userId)` to service.

- [ ] **Step 3: Move UserProfileController.profile() VO construction to UserProfileService**

Add `UserVO getProfile(Long userId)` to service.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: move VO conversion logic from controllers to services"
```

---

### Task 8.2: DTO Validation Enhancement

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/user/dto/RegisterRequest.java`
- Modify: `src/main/java/com/xindai/xindai/modules/user/dto/UserLoginDTO.java`
- Modify: `src/main/java/com/xindai/xindai/modules/agent/controller/ChatController.java`

- [ ] **Step 1: Enhance RegisterRequest validation**

```java
@NotBlank(message = "密码不能为空")
@Size(min = 6, max = 20, message = "密码长度为6-20位")
private String password;

@NotBlank(message = "身份证号不能为空")
@Pattern(regexp = "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$",
         message = "身份证号格式不正确")
private String idCard;
```

- [ ] **Step 2: Add phone pattern to UserLoginDTO**

```java
@NotBlank(message = "手机号不能为空")
@Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
private String phone;
```

- [ ] **Step 3: Add message length validation to ChatController**

```java
@GetMapping("/user/chat/stream")
public SseEmitter chat(@RequestParam String message) {
    if (message == null || message.isBlank()) {
        throw new BusinessException(ErrorCode.PARAM_ERROR);
    }
    if (message.length() > 2000) {
        throw new BusinessException(5001, "消息长度不能超过2000字符");
    }
    // ...
}
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: enhance DTO validation with regex patterns and length limits"
```

---

### Task 8.3: Business Rules Externalization

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/loan/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/xindai/xindai/config/CreditLimitProperties.java` (add interest rates)
- Modify: `src/main/resources/application.yml` (add interest rate config)

- [ ] **Step 1: Add interest rate config to CreditLimitProperties**

Add fields mapping to `credit-limit.rates` section in YAML.

- [ ] **Step 2: Replace hardcoded interest rates in LoanServiceImpl**

Replace the switch statement with:
```java
BigDecimal rate = creditLimitProperties.getRates().get(creditGrade);
```

- [ ] **Step 3: Align max loan amount between config and DTO**

Ensure `LoanApplyDTO` validation reads from `system_config` or a properties class rather than hardcoding `500000`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: externalize hardcoded business rules to configuration properties"
```

---

### Task 8.4: Module Coupling Reduction (Admin Dashboard)

**Files:**
- Modify: `src/main/java/com/xindai/xindai/modules/admin/service/impl/AdminDashboardServiceImpl.java`
- Modify: respective module services to add statistics methods

- [ ] **Step 1: Extract statistics methods to module services**

Move statistics queries from AdminDashboardServiceImpl to:
- `LoanService`: `countByStatus(int status)`, `sumAmountByStatus(int status)`
- `RiskAssessmentService`: `countByRiskLevel(int level)`
- `UserService`: `countByStatus(int status)`

- [ ] **Step 2: Simplify AdminDashboardServiceImpl**

Replace direct mapper/entity imports with service calls.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor: reduce admin module coupling by extracting statistics to module services"
```

---

### Task 8.5: Test Coverage Completion

**Files:**
- Create: all test files listed in the design spec

- [ ] **Step 1: Write ChatControllerTest**

Test SSE streaming endpoint, authentication requirement, input validation.

- [ ] **Step 2: Write CollectionTaskServiceTest**

Test task lifecycle: create → assign → start → add record → complete/close.

- [ ] **Step 3: Write DisbursementServiceTest**

Test disbursement flow: initiate → confirm → verify contract status updated.

- [ ] **Step 4: Write ContractPdfServiceTest**

Test PDF generation produces non-empty byte array.

- [ ] **Step 5: Write WorkflowServiceTest**

Test process start and task completion using Flowable test utilities.

- [ ] **Step 6: Write remaining tests**

CryptoInterceptorTest, RabbitMQEventPublisherTest, MockSmsServiceTest, JwtAuthenticationFilterTest, NotificationEventListenerTest.

- [ ] **Step 7: Run all tests**

Run: `./mvnw test -pl .`
Expected: All tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/test/
git commit -m "test: add comprehensive test coverage for new modules"
```

---

## Final Verification

### Task 9.1: Full Integration Test

- [ ] **Step 1: Start application with dev profile**

Run: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

- [ ] **Step 2: Verify all Swagger endpoints load**

Open: `http://localhost:8080/doc.html`
Check all new endpoints are visible.

- [ ] **Step 3: Run full test suite**

Run: `./mvnw test -pl .`
Expected: All tests pass.

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: complete system refactoring for production readiness"
```
