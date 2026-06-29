# ERP MySQL Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the ERP application's process-local `ErpStore` data with durable MySQL storage while preserving the current REST API and frontend behavior.

**Architecture:** Docker Compose provides MySQL 8.4, Flyway owns schema and seed data, and Spring Data JPA repositories persist each bounded domain. Controllers depend on focused services; document approval uses a single transaction with database locks so document status, inventory, settlements, and messages change atomically.

**Tech Stack:** Java 17, Spring Boot 3.3.6, Spring Data JPA, MySQL 8.4, Flyway, Testcontainers, Docker Compose, JUnit 5, Vue 3

---

## File Structure

Create the following persistence boundaries instead of replacing `ErpStore` with another monolith:

- `com.erp.workspace`: workspace resolution and student isolation.
- `com.erp.user`: user persistence, authentication profile operations, and role menus.
- `com.erp.masterdata`: brand, category, unit, warehouse, customer, supplier, and product persistence.
- `com.erp.document`: document header/item persistence and document lifecycle.
- `com.erp.inventory`: inventory queries and transactional approval.
- `com.erp.settlement`: settlement query and DTO mapping.
- `com.erp.message`: user notification persistence.
- `com.erp.competition`: bug definitions, reports, files, rankings, and operation logs.

JPA entities stay inside their domain package and never leave the service layer. Existing `ErpModels` classes remain API-facing view models until a separate DTO refactor.

## Task 1: Establish a Clean Persistence Test Harness

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/test/java/com/erp/support/MySqlIntegrationTest.java`
- Create: `backend/src/test/resources/application-test.yml`
- Test: `backend/src/test/java/com/erp/PersistenceBootstrapTest.java`

- [ ] **Step 1: Add a failing MySQL bootstrap test**

Create `PersistenceBootstrapTest` that extends the shared container base and verifies the real database product:

```java
package com.erp;

import com.erp.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceBootstrapTest extends MySqlIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void startsWithMySql() {
        var product = jdbcTemplate.queryForObject("select @@version_comment", String.class);
        assertThat(product).containsIgnoringCase("MySQL");
    }
}
```

- [ ] **Step 2: Run the test and verify the missing persistence infrastructure fails**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=PersistenceBootstrapTest test
```

Expected: compilation fails because `MySqlIntegrationTest` and the JPA/Testcontainers dependencies do not exist.

- [ ] **Step 3: Add production and test dependencies**

Add these dependencies to `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4: Create the reusable Testcontainers base**

Create `MySqlIntegrationTest.java`:

```java
package com.erp.support;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class MySqlIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("erp_test")
        .withUsername("erp")
        .withPassword("erp_test_password");

    @Autowired Flyway flyway;

    @BeforeEach
    protected void resetDatabase() {
        flyway.clean();
        flyway.migrate();
    }
}
```

Create `application-test.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    clean-disabled: false
```

- [ ] **Step 5: Run the targeted test**

Run the command from Step 2.

Expected: the container starts; the test then fails only because no Flyway schema exists or passes if Flyway has no migrations yet. Record the exact state before Task 2.

- [ ] **Step 6: Commit the harness**

```powershell
git add backend/pom.xml backend/src/test/java/com/erp/support/MySqlIntegrationTest.java backend/src/test/java/com/erp/PersistenceBootstrapTest.java backend/src/test/resources/application-test.yml
git commit -m "test: add MySQL integration harness"
```

## Task 2: Add Docker Compose, Runtime Configuration, and Flyway Schema

**Files:**
- Create: `compose.yaml`
- Create: `.env.example`
- Modify: `.gitignore`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V1__create_schema.sql`
- Test: `backend/src/test/java/com/erp/PersistenceBootstrapTest.java`

- [ ] **Step 1: Extend the bootstrap test with schema assertions**

Add this test method:

```java
@Test
void flywayCreatesCoreTables() {
    var tables = jdbcTemplate.queryForList(
        "select table_name from information_schema.tables where table_schema = database()",
        String.class
    );
    assertThat(tables).contains(
        "erp_workspace", "sys_user", "master_product", "biz_document",
        "biz_document_item", "inventory_balance", "finance_settlement",
        "test_bug_report", "test_file_submission"
    );
}
```

- [ ] **Step 2: Run the schema test and verify it fails**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=PersistenceBootstrapTest#flywayCreatesCoreTables test
```

Expected: FAIL because the tables do not exist.

- [ ] **Step 3: Add local MySQL Compose configuration**

Create `compose.yaml` with one `mysql` service using `mysql:8.4`, database/user values from `.env`, port `3306`, named volume `erp_mysql_data`, `utf8mb4`, and this health check:

```yaml
healthcheck:
  test: ["CMD-SHELL", "mysqladmin ping -h localhost -u$${MYSQL_USER} -p$${MYSQL_PASSWORD} --silent"]
  interval: 5s
  timeout: 5s
  retries: 20
  start_period: 20s
```

Create `.env.example` with:

```dotenv
MYSQL_DATABASE=erp
MYSQL_USER=erp
MYSQL_PASSWORD=erp_local_password
MYSQL_ROOT_PASSWORD=erp_root_local_password
```

Add `.env` to `.gitignore` without ignoring `.env.example`.

- [ ] **Step 4: Configure Spring for MySQL and Flyway**

Replace the data-related section of `application.yml` with:

```yaml
spring:
  application:
    name: erp-backend
  datasource:
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/${DB_NAME:erp}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:erp}
    password: ${DB_PASSWORD:erp_local_password}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Shanghai
  flyway:
    enabled: true
```

Keep the existing server port and JWT properties.

- [ ] **Step 5: Create the approved schema migration**

Copy the complete MySQL DDL from `docs/superpowers/specs/2026-06-21-mysql-persistence-design.md` into `V1__create_schema.sql` without changing table names, column names, foreign keys, comments, or indexes.

- [ ] **Step 6: Run the schema test**

Run the command from Step 2.

Expected: PASS and Flyway reports migration version `1`.

- [ ] **Step 7: Verify local Compose health**

Run:

```powershell
Copy-Item .env.example .env
docker compose up -d mysql
docker compose ps
```

Expected: service `mysql` becomes `healthy` and the named volume exists.

- [ ] **Step 8: Commit infrastructure and schema**

```powershell
git add compose.yaml .env.example .gitignore backend/src/main/resources/application.yml backend/src/main/resources/db/migration/V1__create_schema.sql backend/src/test/java/com/erp/PersistenceBootstrapTest.java
git commit -m "feat: add MySQL schema and local compose service"
```

## Task 3: Seed Workspaces, Users, Master Data, and Bug Definitions

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__seed_demo_data.sql`
- Create: `backend/src/main/resources/db/migration/V3__seed_bug_definitions.sql`
- Test: `backend/src/test/java/com/erp/PersistenceBootstrapTest.java`

- [ ] **Step 1: Add seed assertions**

Add a test that asserts all 12 current usernames, one system workspace, two student workspaces, the initial brand/category/unit/warehouse/customer/supplier/product, and all 79 rows from `bug-training-definitions.json` exist after migration.

Use these username assertions:

```java
assertThat(jdbcTemplate.queryForList("select username from sys_user", String.class))
    .containsExactlyInAnyOrder(
        "admin", "superadmin", "purchase_manager", "purchase_staff",
        "warehouse_manager", "warehouse_staff", "warehouse_staff_south",
        "sales_manager", "sales_staff", "settlement_manager",
        "student01", "student02"
    );
```

- [ ] **Step 2: Run the seed test and verify it fails**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=PersistenceBootstrapTest#flywaySeedsDemoData test
```

Expected: FAIL with zero seeded users.

- [ ] **Step 3: Create deterministic SQL seed migrations**

`V2__seed_demo_data.sql` must:

- insert system and student workspaces with stable IDs;
- insert all current accounts using one precomputed BCrypt hash for `123456`;
- backfill each student workspace `owner_user_id`;
- insert the current brand, category, unit, warehouse, customer, supplier, and product;
- bind `warehouse_staff` to the seeded warehouse;
- leave `warehouse_staff_south` unbound, matching current behavior.

`V3__seed_bug_definitions.sql` must contain one explicit `INSERT` row per JSON definition and preserve every identifier and defect description.

- [ ] **Step 4: Run all bootstrap tests**

```powershell
cd backend
.\mvnw.cmd -Dtest=PersistenceBootstrapTest test
```

Expected: all bootstrap, schema, and seed assertions pass.

- [ ] **Step 5: Commit seed migrations**

```powershell
git add backend/src/main/resources/db/migration/V2__seed_demo_data.sql backend/src/main/resources/db/migration/V3__seed_bug_definitions.sql backend/src/test/java/com/erp/PersistenceBootstrapTest.java
git commit -m "feat: seed ERP demo data with Flyway"
```

## Task 4: Persist Workspaces and Authentication

**Files:**
- Create: `backend/src/main/java/com/erp/workspace/WorkspaceEntity.java`
- Create: `backend/src/main/java/com/erp/workspace/WorkspaceRepository.java`
- Create: `backend/src/main/java/com/erp/workspace/WorkspaceService.java`
- Create: `backend/src/main/java/com/erp/user/UserEntity.java`
- Create: `backend/src/main/java/com/erp/user/UserRepository.java`
- Create: `backend/src/main/java/com/erp/user/UserService.java`
- Modify: `backend/src/main/java/com/erp/web/AuthController.java`
- Test: `backend/src/test/java/com/erp/AuthPersistenceIntegrationTest.java`

- [ ] **Step 1: Write persistence-first authentication tests**

Cover login from seeded MySQL data, password change surviving a Spring context restart, avatar persistence, disabled user rejection, and role menu compatibility. Use MockMvc and assert the existing `code/message/data` response shape.

The persistence assertion must query the database after the API call:

```java
assertThat(jdbcTemplate.queryForObject(
    "select password_hash from sys_user where username = 'admin'",
    String.class
)).isNotBlank();
```

- [ ] **Step 2: Run the authentication test and verify it fails**

```powershell
cd backend
.\mvnw.cmd -Dtest=AuthPersistenceIntegrationTest test
```

Expected: FAIL because `AuthController` still reads `ErpStore` memory users.

- [ ] **Step 3: Implement workspace and user entities/repositories**

Map enum fields with `@Enumerated(EnumType.STRING)`, timestamps with `LocalDateTime`, avatar with `@Lob`, user versioning only where concurrent profile updates require it, and repository lookups:

```java
Optional<UserEntity> findByUsernameAndStatus(String username, Status status);
List<UserEntity> findByRoleCodeAndStatus(RoleCode roleCode, Status status);
```

`WorkspaceService.workspaceIdFor(user)` returns the user's persisted `workspace_id`; it never derives isolation from nullable IDs.

- [ ] **Step 4: Implement `UserService` and rewire `AuthController`**

`UserService` must provide `authenticate`, `currentUser`, `changePassword`, `updateAvatar`, `menus`, and `warehouseStaff`. Preserve the current Chinese validation messages and response properties.

- [ ] **Step 5: Run authentication and existing application tests**

```powershell
cd backend
.\mvnw.cmd -Dtest=AuthPersistenceIntegrationTest,ErpApplicationTests test
```

Expected: all selected tests pass without reading in-memory users.

- [ ] **Step 6: Commit authentication persistence**

```powershell
git add backend/src/main/java/com/erp/workspace backend/src/main/java/com/erp/user backend/src/main/java/com/erp/web/AuthController.java backend/src/test/java/com/erp/AuthPersistenceIntegrationTest.java
git commit -m "feat: persist users and workspaces"
```

## Task 5: Persist Master Data

**Files:**
- Create: `backend/src/main/java/com/erp/masterdata/BrandEntity.java`
- Create: `backend/src/main/java/com/erp/masterdata/CategoryEntity.java`
- Create: `backend/src/main/java/com/erp/masterdata/UnitEntity.java`
- Create: `backend/src/main/java/com/erp/masterdata/WarehouseEntity.java`
- Create: `backend/src/main/java/com/erp/masterdata/CustomerEntity.java`
- Create: `backend/src/main/java/com/erp/masterdata/SupplierEntity.java`
- Create: `backend/src/main/java/com/erp/masterdata/ProductEntity.java`
- Create: `backend/src/main/java/com/erp/masterdata/BrandRepository.java`
- Create: `backend/src/main/java/com/erp/masterdata/CategoryRepository.java`
- Create: `backend/src/main/java/com/erp/masterdata/UnitRepository.java`
- Create: `backend/src/main/java/com/erp/masterdata/WarehouseRepository.java`
- Create: `backend/src/main/java/com/erp/masterdata/CustomerRepository.java`
- Create: `backend/src/main/java/com/erp/masterdata/SupplierRepository.java`
- Create: `backend/src/main/java/com/erp/masterdata/ProductRepository.java`
- Create: `backend/src/main/java/com/erp/masterdata/MasterDataService.java`
- Modify: `backend/src/main/java/com/erp/web/MasterDataController.java`
- Test: `backend/src/test/java/com/erp/MasterDataPersistenceIntegrationTest.java`

- [ ] **Step 1: Write master-data persistence tests**

Test list/search, create, update, status change, product import, workspace isolation, duplicate code rejection, cross-workspace reference rejection, and data survival after service reload. Assert database rows in the seven master tables.

- [ ] **Step 2: Run the test and verify it fails**

```powershell
cd backend
.\mvnw.cmd -Dtest=MasterDataPersistenceIntegrationTest test
```

Expected: FAIL because the controller still delegates to `ErpStore`.

- [ ] **Step 3: Implement entities and repositories**

Every repository query must include `workspaceId`. Product writes must load category, brand, and unit by both ID and workspace. Use the database unique constraints as the final duplicate-code guard and translate `DataIntegrityViolationException` to the current Chinese business error format.

- [ ] **Step 4: Implement dynamic-type compatibility in `MasterDataService`**

Keep accepted type values exactly: `brand`, `category`, `unit`, `warehouse`, `customer`, `supplier`, and `product`. Return view models with the current property names, including category/brand/unit names and warehouse staff assignment.

- [ ] **Step 5: Rewire `MasterDataController` and run tests**

```powershell
cd backend
.\mvnw.cmd -Dtest=MasterDataPersistenceIntegrationTest,ErpCoreFlowIntegrationTest test
```

Expected: master-data tests pass; any remaining core-flow failures identify the still-unmigrated document boundary rather than master data.

- [ ] **Step 6: Commit master-data persistence**

```powershell
git add backend/src/main/java/com/erp/masterdata backend/src/main/java/com/erp/web/MasterDataController.java backend/src/test/java/com/erp/MasterDataPersistenceIntegrationTest.java
git commit -m "feat: persist ERP master data"
```

## Task 6: Persist Documents and Document Items

**Files:**
- Create: `backend/src/main/java/com/erp/document/DocumentEntity.java`
- Create: `backend/src/main/java/com/erp/document/DocumentItemEntity.java`
- Create: `backend/src/main/java/com/erp/document/DocumentRepository.java`
- Create: `backend/src/main/java/com/erp/document/DocumentItemRepository.java`
- Create: `backend/src/main/java/com/erp/document/DocumentService.java`
- Modify: `backend/src/main/java/com/erp/web/DocumentController.java`
- Test: `backend/src/test/java/com/erp/DocumentPersistenceIntegrationTest.java`

- [ ] **Step 1: Write document persistence tests**

Cover create, update, detail, submit, delete, manager visibility, staff ownership, student workspace isolation, document-number uniqueness, return original-document validation, and remaining return quantity.

- [ ] **Step 2: Run the test and verify it fails**

```powershell
cd backend
.\mvnw.cmd -Dtest=DocumentPersistenceIntegrationTest test
```

Expected: FAIL because documents are still process-local.

- [ ] **Step 3: Implement document entities and repositories**

Use `@Version` on `DocumentEntity`. Map items with `cascade = CascadeType.ALL` and `orphanRemoval = true`. Persist the approved snapshot columns so later master-data edits do not rewrite historical documents.

Add a locked lookup used by approval:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select d from DocumentEntity d left join fetch d.items where d.id = :id")
Optional<DocumentEntity> findByIdForUpdate(@Param("id") Long id);
```

- [ ] **Step 4: Implement lifecycle and visibility rules in `DocumentService`**

Preserve `DRAFT → PENDING → APPROVED/REJECTED`, staff ownership, manager visibility, warehouse matching, workspace isolation, auto-generated document numbers, snapshot mapping, and existing Chinese error messages.

- [ ] **Step 5: Rewire `DocumentController` and run tests**

```powershell
cd backend
.\mvnw.cmd -Dtest=DocumentPersistenceIntegrationTest test
```

Expected: all document tests pass and database rows remain after repository/service reload.

- [ ] **Step 6: Commit document persistence**

```powershell
git add backend/src/main/java/com/erp/document backend/src/main/java/com/erp/web/DocumentController.java backend/src/test/java/com/erp/DocumentPersistenceIntegrationTest.java
git commit -m "feat: persist ERP documents"
```

## Task 7: Make Inventory Approval, Settlements, and Messages Transactional

**Files:**
- Create: `backend/src/main/java/com/erp/inventory/InventoryBalanceEntity.java`
- Create: `backend/src/main/java/com/erp/inventory/InventoryBalanceRepository.java`
- Create: `backend/src/main/java/com/erp/inventory/InventoryService.java`
- Create: `backend/src/main/java/com/erp/settlement/SettlementEntity.java`
- Create: `backend/src/main/java/com/erp/settlement/SettlementRepository.java`
- Create: `backend/src/main/java/com/erp/settlement/SettlementService.java`
- Create: `backend/src/main/java/com/erp/message/MessageEntity.java`
- Create: `backend/src/main/java/com/erp/message/MessageRepository.java`
- Create: `backend/src/main/java/com/erp/message/MessageService.java`
- Modify: `backend/src/main/java/com/erp/web/InventoryController.java`
- Modify: `backend/src/main/java/com/erp/web/SettlementController.java`
- Modify: `backend/src/main/java/com/erp/web/SystemController.java`
- Test: `backend/src/test/java/com/erp/InventoryTransactionIntegrationTest.java`

- [ ] **Step 1: Write transaction and concurrency tests**

Test purchase receipt, purchase return, sales outbound, sales return, transfer, insufficient inventory rollback, duplicate approval, two concurrent outbound approvals, settlement uniqueness, warehouse-specific messages, and failure rollback. The rollback assertion must prove document status, stock, settlement count, and message count are all unchanged.

- [ ] **Step 2: Run the transaction tests and verify they fail**

```powershell
cd backend
.\mvnw.cmd -Dtest=InventoryTransactionIntegrationTest test
```

Expected: FAIL because approval is not database-transactional.

- [ ] **Step 3: Implement locked inventory repositories**

Provide a pessimistic lookup by the unique inventory key:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<InventoryBalanceEntity> findByWorkspaceIdAndWarehouseIdAndProductId(
    Long workspaceId, Long warehouseId, Long productId
);
```

Use `actualQuantity` as the persisted value. Calculate available quantity from open outbound document items in a repository projection.

- [ ] **Step 4: Implement atomic approval**

Mark `InventoryService.approve(documentId, auditorId)` as `@Transactional`. Lock the document first, then all inventory keys in deterministic warehouse/product order, validate stock and return quantities, update balances, update the document, create one settlement when applicable, and create messages before returning.

Reject and transfer approval are also transactional. Map lock conflicts to `BusinessException("数据已被其他操作更新，请刷新后重试。")`.

- [ ] **Step 5: Rewire controllers and run transaction tests**

```powershell
cd backend
.\mvnw.cmd -Dtest=InventoryTransactionIntegrationTest,ErpCoreFlowIntegrationTest test
```

Expected: all core ERP tests pass against MySQL.

- [ ] **Step 6: Commit the transactional core**

```powershell
git add backend/src/main/java/com/erp/inventory backend/src/main/java/com/erp/settlement backend/src/main/java/com/erp/message backend/src/main/java/com/erp/web/InventoryController.java backend/src/main/java/com/erp/web/SettlementController.java backend/src/main/java/com/erp/web/SystemController.java backend/src/test/java/com/erp/InventoryTransactionIntegrationTest.java
git commit -m "feat: persist inventory settlement and messages"
```

## Task 8: Persist the Testing Competition

**Files:**
- Create: `backend/src/main/java/com/erp/competition/BugDefinitionEntity.java`
- Create: `backend/src/main/java/com/erp/competition/BugReportEntity.java`
- Create: `backend/src/main/java/com/erp/competition/FileSubmissionEntity.java`
- Create: `backend/src/main/java/com/erp/competition/RankingHistoryEntity.java`
- Create: `backend/src/main/java/com/erp/competition/OperationLogEntity.java`
- Create: `backend/src/main/java/com/erp/competition/BugDefinitionRepository.java`
- Create: `backend/src/main/java/com/erp/competition/BugReportRepository.java`
- Create: `backend/src/main/java/com/erp/competition/FileSubmissionRepository.java`
- Create: `backend/src/main/java/com/erp/competition/RankingHistoryRepository.java`
- Create: `backend/src/main/java/com/erp/competition/OperationLogRepository.java`
- Create: `backend/src/main/java/com/erp/competition/CompetitionService.java`
- Modify: `backend/src/main/java/com/erp/web/CompetitionController.java`
- Test: `backend/src/test/java/com/erp/CompetitionPersistenceIntegrationTest.java`

- [ ] **Step 1: Write persistent competition tests**

Cover defect publishing, student soft deletion, report submission/review, file metadata persistence, super-admin file review, operation logs, ranking history, rankings, workspace isolation, score range constraints, and active defect behavior after service reload.

- [ ] **Step 2: Run the test and verify it fails**

```powershell
cd backend
.\mvnw.cmd -Dtest=CompetitionPersistenceIntegrationTest test
```

Expected: FAIL because competition data still resides in memory.

- [ ] **Step 3: Implement entities, repositories, and service**

Keep bug IDs as strings. Soft-delete students by setting `sys_user.status = DISABLED`; exclude disabled students from lists and login. Preserve reports, files, histories, logs, and student workspaces.

Report and file review must be transactional. Scores outside `0..100` fail before persistence. Approved report and file scores feed the ranking query.

- [ ] **Step 4: Make file/database writes compensating**

Save uploads under `${ERP_UPLOAD_DIR:uploads}/competition/<username>/`. If file writing fails, do not insert metadata. If the database transaction fails after writing, delete the newly written file in a catch/finally compensation path and rethrow the original exception.

- [ ] **Step 5: Rewire `CompetitionController` and run tests**

```powershell
cd backend
.\mvnw.cmd -Dtest=CompetitionPersistenceIntegrationTest,TrainingCompetitionIntegrationTest test
```

Expected: all competition tests pass against MySQL and persisted rows survive service reload.

- [ ] **Step 6: Commit competition persistence**

```powershell
git add backend/src/main/java/com/erp/competition backend/src/main/java/com/erp/web/CompetitionController.java backend/src/test/java/com/erp/CompetitionPersistenceIntegrationTest.java
git commit -m "feat: persist testing competition data"
```

## Task 9: Remove the In-Memory Store and Complete Regression Coverage

**Files:**
- Delete: `backend/src/main/java/com/erp/store/ErpStore.java`
- Modify: `backend/src/main/java/com/erp/domain/ErpModels.java`
- Modify: `backend/src/test/java/com/erp/ErpApplicationTests.java`
- Modify: `backend/src/test/java/com/erp/ErpCoreFlowIntegrationTest.java`
- Modify: `backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java`
- Test: `backend/src/test/java/com/erp/PersistenceRestartIntegrationTest.java`

- [ ] **Step 1: Add a restart persistence regression test**

Create data through the API, close and recreate the Spring application context against the same container database, then assert the user change, master record, document, inventory, settlement, report, and file metadata still exist.

- [ ] **Step 2: Run the restart test and verify the remaining memory dependency fails**

```powershell
cd backend
.\mvnw.cmd -Dtest=PersistenceRestartIntegrationTest test
```

Expected: FAIL if any endpoint still depends on `ErpStore` initialization.

- [ ] **Step 3: Remove all `ErpStore` references**

Run:

```powershell
rg -n 'ErpStore|new LinkedHashMap|new ArrayList' backend/src/main/java/com/erp
```

Delete `ErpStore.java` only after every controller uses its focused service. Keep collection creation used only for response assembly; remove all process-local business state and ID counters.

- [ ] **Step 4: Adapt existing tests to the shared MySQL base**

Make the three existing integration test classes extend `MySqlIntegrationTest`. Replace assumptions that a new Spring context automatically resets memory with the Flyway reset performed before each test. Preserve all current assertions.

- [ ] **Step 5: Run the full backend suite**

```powershell
cd backend
.\mvnw.cmd test
```

Expected: all original 18 tests plus the new persistence, constraint, transaction, concurrency, and restart tests pass.

- [ ] **Step 6: Commit store removal and regression updates**

```powershell
git add -A backend/src/main/java/com/erp/store backend/src/main/java/com/erp/domain backend/src/test/java/com/erp
git commit -m "refactor: remove in-memory ERP store"
```

## Task 10: Update Documentation and Verify the Complete System

**Files:**
- Modify: `README.md`
- Modify: `docs/project-showcase.md`
- Create: `docs/database.md`

- [ ] **Step 1: Update operational documentation**

Document these exact flows:

```powershell
Copy-Item .env.example .env
docker compose up -d mysql
cd backend
.\mvnw.cmd spring-boot:run
```

Remove all statements that data resets on backend restart. Document Flyway, `.env`, database backup/reset, upload metadata persistence, Testcontainers prerequisites, and `docker compose down -v` as the explicit destructive local reset command.

- [ ] **Step 2: Run document validation**

```powershell
rg -n '内存数据|重启.*重置|不依赖数据库|待补充|待完善' README.md docs/project-showcase.md docs/database.md
```

Expected: no stale in-memory claims and no placeholders.

- [ ] **Step 3: Run the full verification matrix**

```powershell
docker compose up -d mysql
cd backend
.\mvnw.cmd test
cd ..\frontend
npm.cmd test -- --run
npm.cmd run build
```

Expected:

- Docker reports MySQL healthy.
- All backend tests pass against MySQL Testcontainers.
- All frontend tests pass.
- Vite production build exits with code 0; the existing chunk-size warning is non-blocking.

- [ ] **Step 4: Verify persistence manually**

Start the backend, create a master record and document through the UI, stop and restart only the backend, then confirm the records still exist. Run:

```powershell
docker compose exec mysql mysql -uerp -perp_local_password erp -e "select count(*) as users from sys_user; select count(*) as documents from biz_document;"
```

Expected: counts remain unchanged across the backend restart.

- [ ] **Step 5: Review only planned changes**

```powershell
git diff --check
git status --short
rg -n 'ErpStore' backend/src/main backend/src/test
```

Expected: no whitespace errors, no remaining `ErpStore` references, and no unrelated user files included in commits.

- [ ] **Step 6: Commit documentation**

```powershell
git add README.md docs/project-showcase.md docs/database.md
git commit -m "docs: document MySQL persistence workflow"
```
