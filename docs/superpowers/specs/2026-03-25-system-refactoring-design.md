# Xindai Backend System Refactoring Design

**Date**: 2026-03-25
**Status**: Approved
**Scope**: Architecture + Feature completeness refactoring for production readiness

---

## 1. Overview

The xindai backend (multi-modal intelligent risk control system) requires comprehensive refactoring to achieve production-grade quality. This design covers 8 layers of improvements, from infrastructure to code quality, using a bottom-up incremental approach.

### 1.1 Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| System target | Production-grade | Full enterprise features required |
| Message Queue | RabbitMQ | Best fit for async decoupling in loan systems |
| Workflow Engine | Flowable | Full BPMN support, visual designer, mature community |
| SMS Service | Mock implementation | Interface designed for real integration, mock for now |
| Email Service | Spring Mail + Thymeleaf | SMTP-based, template rendering |
| Data Encryption | MyBatis interceptor | Transparent AES-256 auto encrypt/decrypt |
| E-Contract | PDF generation + mock seal | iText/OpenPDF for contract PDFs |
| Collection Module | Basic collection | Task assignment, records, status tracking |
| Implementation | Layer-by-layer incremental | Bottom-up dependency order |

### 1.2 Implementation Layers (Execution Order)

1. Infrastructure (RabbitMQ, encryption, multi-env config, Flyway)
2. Security fixes (JWT, auth, transactions, locks)
3. Event-driven refactoring (events, publisher, listeners, decoupling)
4. Flowable workflow + disbursement flow
5. Business feature completion (collection, KYC, e-contract, reports)
6. Notification enhancement (multi-channel, email, SMS mock, reminders)
7. Cache optimization & performance (Redis, N+1, pagination, indexes)
8. Code quality & tests (layering, validation, decoupling, test coverage)

---

## 2. Layer 1: Infrastructure

### 2.1 Multi-Environment Configuration

Split `application.yml` into:

- **`application.yml`** — Shared config: MyBatis-Plus settings, Flyway, log format, server port
- **`application-dev.yml`** — Development: local MySQL/Redis, SQL logging enabled, Swagger open
- **`application-prod.yml`** — Production: env var injection, SQL logging off, Swagger disabled
- **`application-test.yml`** — Test: already exists, supplement missing configs

Remove all hardcoded sensitive defaults. Required env vars (missing = startup failure):
- `DB_PASSWORD`, `JWT_SECRET`, `ENCRYPTION_KEY`

### 2.2 RabbitMQ Integration

New dependency: `spring-boot-starter-amqp`

**Exchange**: `xindai.direct` (Direct Exchange)

**Queue definitions**:

| Queue | Purpose | Consumer |
|-------|---------|----------|
| `loan.application.submitted` | Loan application submitted | RiskAssessmentListener, NotificationListener |
| `loan.application.approved` | Loan approved | DisbursementListener, ContractListener, NotificationListener |
| `loan.application.rejected` | Loan rejected | NotificationListener |
| `loan.repayment.completed` | Repayment completed | CreditLimitListener, NotificationListener |
| `loan.overdue.detected` | Overdue detected | CollectionListener, NotificationListener |
| `risk.assessment.completed` | Risk assessment done | ApplicationStatusListener |

**Configuration**:
- JSON message serialization via `Jackson2JsonMessageConverter`
- Dead letter exchange for all queues
- Manual ACK mode for reliability

### 2.3 MyBatis Encryption Interceptor

New package: `common/mybatis/`

**Components**:
- `@CryptoField(algorithm = "AES")` — Field-level annotation
- `CryptoInterceptor` — MyBatis-Plus InnerInterceptor for auto encrypt/decrypt
- `AesCryptoTypeHandler` — AES-256 encrypt on write, decrypt on read
- Query parameter auto-encryption for encrypted field lookups

**Encrypted fields**: User.phone, User.id_card

**Key management**: AES key from env var `ENCRYPTION_KEY`, validated at startup.

### 2.4 Flyway Migrations (V8-V12)

| Version | Content |
|---------|---------|
| V8 | bank_account, disbursement_record tables; user_profile identity_status field |
| V9 | collection_task, collection_record tables |
| V10 | contract_template table |
| V11 | Reserved for additional fields |
| V12 | Composite indexes for performance optimization |

---

## 3. Layer 2: Security Fixes

### 3.1 JWT Filter Refactoring

**Problem**: Two JWT filters process all tokens without differentiation; roles hardcoded as ROLE_USER.

**Solution**: Merge into single `JwtAuthenticationFilter`:
- Add `userType` claim to JWT tokens (`USER`/`ADMIN`/`ENTERPRISE`)
- Set roles based on userType:
  - USER → ROLE_USER
  - ADMIN → ROLE_ADMIN or ROLE_SUPER_ADMIN (based on admin_user.role)
  - ENTERPRISE → ROLE_ENTERPRISE + sub-roles
- Delete `EnterpriseJwtAuthenticationFilter`

**Token structure**: `{sub: userId, userType: "USER", enterpriseId: null/xxx, roles: [...], exp: ...}`

### 3.2 Admin Authorization Fix

- `AdminAuthController.register()` → `@PreAuthorize("hasRole('SUPER_ADMIN')")`
- JWT generation for admin uses admin_user.role to set ADMIN/SUPER_ADMIN
- Verify all admin endpoints have proper `@PreAuthorize`

### 3.3 Transaction Standardization

All `@Transactional` annotations must include `rollbackFor = Exception.class`.

Affected files: LoanServiceImpl, AdminApplicationServiceImpl, and any other bare `@Transactional`.

### 3.4 Sensitive Config Validation

- Remove hardcoded defaults for JWT_SECRET and DB_PASSWORD
- New `SensitiveConfigValidator` with `@PostConstruct` to validate required env vars at startup
- Production profile disables Swagger and SQL stdout logging

### 3.5 Distributed Lock Enhancement

- `CreditLimitService.getOrCreateCreditLimit()` and `updateCreditLimitByRiskScore()`: add Redis distributed lock
- Lua script for atomic check-and-delete lock release (replaces simple delete)
- Apply same fix to `RiskAssessmentServiceImpl` lock

### 3.6 Other Security Items

- `GlobalExceptionHandler`: add handlers for `AccessDeniedException`, `HttpMessageNotReadableException`
- `OperateLogAspect`: add trusted proxy check for X-Forwarded-For
- Production: Swagger disabled (`springdoc.api-docs.enabled: false`), SQL logging off

---

## 4. Layer 3: Event-Driven Refactoring

### 4.1 Domain Events

New package: `common/event/`

Base class `DomainEvent`: eventId (UUID), occurredAt, traceId (from MDC).

Concrete events:
- `LoanApplicationSubmittedEvent` — applicationId, userId, amount, term
- `LoanApplicationApprovedEvent` — applicationId, userId, amount, approvedAmount, reviewNote
- `LoanApplicationRejectedEvent` — applicationId, userId, reason
- `RiskAssessmentCompletedEvent` — assessmentId, userId, applicationId, riskScore, decision
- `RepaymentCompletedEvent` — contractId, userId, period, amount
- `LoanOverdueDetectedEvent` — contractId, userId, overdueDays, overdueAmount
- `DisbursementCompletedEvent` — contractId, applicationId, userId, amount, disbursedAt

### 4.2 Event Publisher

Interface: `EventPublisher` with method `publish(DomainEvent event)`

Implementation: `RabbitMQEventPublisher` — publishes to RabbitMQ exchange.

Fallback: Local `ApplicationEventPublisher` when MQ unavailable.

### 4.3 Event Listeners

| Listener | Event | Action |
|----------|-------|--------|
| `RiskAssessmentListener` | LoanApplicationSubmittedEvent | Trigger risk assessment |
| `ApplicationStatusListener` | RiskAssessmentCompletedEvent | Update application status |
| `DisbursementListener` | LoanApplicationApprovedEvent | Initiate disbursement |
| `ContractListener` | LoanApplicationApprovedEvent | Generate loan contract |
| `CreditLimitListener` | RepaymentCompletedEvent | Restore available credit |
| `CollectionListener` | LoanOverdueDetectedEvent | Create collection task |
| `NotificationListener` | All business events | Send appropriate notifications |

### 4.4 Module Decoupling Changes

- `LoanServiceImpl.apply()` → no longer calls `RiskAssessmentService` directly, publishes event
- `LoanServiceImpl.repay()` → publishes event instead of directly manipulating credit limit
- `OverdueCheckTask` → publishes `LoanOverdueDetectedEvent`
- risk module → accesses user data through `UserService` interface, not UserMapper directly

---

## 5. Layer 4: Flowable Workflow + Disbursement

### 5.1 Flowable Integration

Dependency: `flowable-spring-boot-starter` (process engine only)

Process definitions as BPMN 2.0 XML in `resources/processes/`, auto-deployed by Flowable.

### 5.2 Loan Approval Process

```
[Start] -> Auto Risk Assessment -> Gateway
                                    |-- Low Risk -> Auto Approve -> [End]
                                    |-- Medium Risk -> Manual Review -> Gateway
                                    |                                    |-- Approve -> [End]
                                    |                                    |-- Reject -> [End]
                                    |                                    |-- Return -> Back to Risk Assessment
                                    |-- High Risk -> Auto Reject -> [End]
```

Process key: `loanApprovalProcess`

Process variables: applicationId, userId, amount, riskScore, riskDecision, approvedAmount, reviewNote

### 5.3 Workflow Module

New module: `modules/workflow/`

```
modules/workflow/
  service/WorkflowService.java
  service/impl/WorkflowServiceImpl.java
  dto/StartProcessDTO.java, CompleteTaskDTO.java, TaskVO.java
  controller/WorkflowController.java
  config/FlowableConfig.java
```

### 5.4 Disbursement Service

New service: `modules/loan/service/DisbursementService`

**Flow**: Approved -> Generate contract PDF -> Wait for admin confirmation -> Execute disbursement -> Update status to DISBURSED -> Generate repayment plans

**New tables** (V8 migration):

```sql
bank_account (id, user_id, bank_name, account_no, account_name, is_default, status, created_at, updated_at)
disbursement_record (id, contract_id, application_id, user_id, amount, bank_account_id, status, transaction_no, completed_at, failed_reason, created_at)
```

**Service interface**:
```java
DisbursementRecord initiateDisbursement(Long contractId, Long bankAccountId);
DisbursementRecord confirmDisbursement(Long disbursementId);
DisbursementRecord getDisbursementStatus(Long disbursementId);
List<DisbursementRecord> getDisbursementsByContract(Long contractId);
```

---

## 6. Layer 5: Business Feature Completion

### 6.1 Collection Module

New module: `modules/collection/`

**Structure**:
```
modules/collection/
  controller/CollectionController.java
  service/CollectionTaskService.java
  service/impl/CollectionTaskServiceImpl.java
  entity/CollectionTask.java, CollectionRecord.java
  mapper/CollectionTaskMapper.java, CollectionRecordMapper.java
  dto/CollectionTaskVO.java, CollectionRecordVO.java, CollectionTaskQueryDTO.java
  enums/CollectionTaskStatus.java, CollectionMethod.java
```

**Task flow**: PENDING -> ASSIGNED -> IN_PROGRESS -> COMPLETED / CLOSED

**Tables** (V9 migration):

```sql
collection_task (id, contract_id, user_id, overdue_amount, overdue_days, collector_id, status, priority, deadline, created_at, updated_at)
collection_record (id, task_id, collector_id, method, content, result, next_follow_up_date, created_at)
```

### 6.2 KYC Enhancement

New: `client/kyc/KycService` interface + `MockKycServiceImpl`

- Verify name + ID card consistency (mock)
- Status: UNVERIFIED -> PENDING -> VERIFIED / FAILED
- New field on user_profile: `identity_status`, `identity_verified_at` (V8 migration)
- Loan application requires `identity_status == VERIFIED`

### 6.3 Electronic Contract (PDF + Mock Seal)

Dependency: OpenPDF (or iText 7)

`ContractPdfService`:
- `generateContract(LoanContract, User)` -> PDF bytes
- Contract content: borrower info, amount, rate, term, repayment schedule, signature areas
- Seal: pre-stored signature image + platform stamp, overlaid on PDF
- Upload to file service, store path in `loan_contract.document_url`

Contract template table (V10 migration):

```sql
contract_template (id, name, type, content, version, status, created_at, updated_at)
```

### 6.4 Report Export

Using existing `easyexcel` dependency:

| Report | Endpoint | Format |
|--------|----------|--------|
| Loan ledger | `GET /api/v1/admin/reports/loans/export` | Excel |
| Overdue report | `GET /api/v1/admin/reports/overdue/export` | Excel |
| Risk statistics | `GET /api/v1/admin/reports/risk/export` | Excel |
| Collection performance | `GET /api/v1/admin/reports/collection/export` | Excel |
| Loan contract | `GET /api/v1/loan/contracts/{id}/pdf` | PDF |

New service: `ReportService` + `ReportServiceImpl` in admin module.

---

## 7. Layer 6: Notification Enhancement

### 7.1 Multi-Channel Architecture

Strategy + Factory pattern:

```java
public interface NotificationChannel {
    boolean supports(NotificationType type);
    void send(NotificationMessage message);
}
```

Channels:
- `InAppNotificationChannel` — existing DB notification logic
- `EmailNotificationChannel` — Spring Mail + Thymeleaf templates
- `SmsNotificationChannel` — Mock implementation with real-service interface

### 7.2 Spring Mail Integration

Dependencies: `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf`

SMTP config in `application-prod.yml` (env var injection).

Email templates in `resources/templates/email/`:
- loan-approved.html, loan-rejected.html
- repayment-reminder.html, overdue-notice.html
- contract-generated.html

### 7.3 SMS Mock Implementation

```java
public interface SmsService {
    SendResult send(String phone, String templateCode, Map<String, String> params);
}

@Service
@ConditionalOnProperty(name = "sms.enabled", havingValue = "false", matchIfMissing = true)
public class MockSmsServiceImpl implements SmsService { /* logs only */ }
```

### 7.4 Notification Scene Mapping

| Event | In-App | Email | SMS |
|-------|--------|-------|-----|
| Application submitted | Y | N | N |
| Risk assessment completed | Y | N | N |
| Loan approved | Y | Y | Y |
| Loan rejected | Y | Y | N |
| Disbursement completed | Y | Y | Y |
| Repayment reminder (3 days before) | Y | Y | Y |
| Repayment completed | Y | N | N |
| Overdue notice | Y | Y | Y |
| Collection notice | Y | Y | Y |
| Contract generated | Y | Y | N |

### 7.5 Scheduled Repayment Reminder

New task: `RepaymentReminderTask`
- Cron: daily at 08:00
- Query repayment plans due within 3 days
- Send multi-channel reminder
- Deduplicate: one reminder per user per contract per day

---

## 8. Layer 7: Cache & Performance

### 8.1 Redis Cache Completion

| Cache Name | TTL | Object | Eviction Trigger |
|-----------|-----|--------|-----------------|
| creditLimit | 5min | User credit limit | apply/repay |
| userProfile | 10min | User profile | profile update |
| blacklist | 1h | Blacklist data | add/remove |
| riskAssessment | 30min | Latest assessment | new assessment |
| dashboardStats | 5min | Dashboard stats | periodic |
| systemConfig | 30min | System config | config update |

### 8.2 N+1 Query Fix

`LoanServiceImpl.getPendingRepayment()`: replace per-plan contract query with batch `selectBatchIds`.

### 8.3 Pagination

Add page/size parameters to unpaginated list endpoints:

| Endpoint | Current | Target |
|----------|---------|--------|
| GET /loan/applications | List | Page<VO> |
| GET /loan/contracts | List | Page<VO> |
| GET /loan/repayment-plans | List | Page<VO> |
| GET /risk/assessments | LIMIT 20 | Page<VO> |

### 8.4 Index Optimization (V12 Migration)

```sql
CREATE INDEX idx_loan_app_user_status ON loan_application(user_id, status);
CREATE INDEX idx_repayment_contract_status ON repayment_plan(contract_id, status);
CREATE INDEX idx_risk_user_created ON risk_assessment(user_id, created_at DESC);
CREATE INDEX idx_notification_user_read ON notification(user_id, is_read, created_at DESC);
CREATE INDEX idx_collection_task_status ON collection_task(status, priority);
```

---

## 9. Layer 8: Code Quality & Tests

### 9.1 Layering Violations Fix

Move VO conversion from controllers to services:
- `RiskController.toVO()` → `RiskAssessmentService`
- `EnterpriseAuthController.profile()` → `EnterpriseAuthService`
- `UserProfileController.profile()` → `UserProfileService`

### 9.2 DTO Validation Enhancement

- `RegisterRequest`: password length (6-20), idCard regex (18 digits)
- `UserLoginDTO`: phone `@Pattern` validation
- `LoanApplyDTO`: amount validation against system_config.max_loan_amount
- Chat message: max length 2000 chars

### 9.3 Module Coupling Reduction

- risk module: use `UserService` interface instead of `UserMapper`/`UserProfileMapper`
- admin module: extract statistics methods to respective module services

### 9.4 Business Rules Externalization

- Interest rates: read from `CreditLimitProperties` (already configured but unused)
- Credit limit factors: ensure all read from properties
- System config: align `max_loan_amount` between DB and DTO validation

### 9.5 Test Coverage

New test files:

| Module | Test File | Coverage |
|--------|-----------|----------|
| agent | ChatControllerTest | SSE streaming, auth, params |
| agent | ChatMemoryServiceTest | Memory management, history |
| agent | ToolContextTest | ThreadLocal lifecycle |
| notification | NotificationServiceTest | Multi-channel, templates |
| file | FileStorageServiceTest | Upload/download, path traversal |
| collection | CollectionTaskServiceTest | Task lifecycle |
| security | JwtAuthenticationFilterTest | Multi-type token parsing |
| disbursement | DisbursementServiceTest | Disbursement flow |
| contract | ContractPdfServiceTest | PDF generation |
| workflow | WorkflowServiceTest | Flowable process/task |

---

## 10. New Dependencies Summary

```xml
<!-- RabbitMQ -->
<dependency>spring-boot-starter-amqp</dependency>

<!-- Flowable -->
<dependency>flowable-spring-boot-starter</dependency>

<!-- Email -->
<dependency>spring-boot-starter-mail</dependency>

<!-- Thymeleaf (email templates) -->
<dependency>spring-boot-starter-thymeleaf</dependency>

<!-- PDF generation -->
<dependency>com.github.librepdf:openpdf</dependency>

<!-- AES encryption (if not in hutool) -->
<!-- hutool-all already includes crypto utilities -->
```

---

## 11. New Module Structure

```
modules/
  workflow/         # NEW - Flowable integration
  collection/       # NEW - Collection management
  (existing modules modified)
common/
  event/            # NEW - Domain events
  mybatis/          # NEW - Crypto interceptor
client/
  kyc/              # NEW - KYC service interface
```

---

## 12. Estimated Scope

~120+ new/modified files across all layers.
