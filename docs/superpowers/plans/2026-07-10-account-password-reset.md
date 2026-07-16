# Account Password Reset Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair missing password hashes in historical databases and let the super administrator reset a student and every linked ERP account to `123456`, while enforcing account status and request validation.

**Architecture:** Keep `sys_user.password_hash` as the only persisted password field. A Flyway V5 migration repairs historical schemas, `ErpStore` owns the linked-account reset operation, service/controller layers expose it to `SUPER_ADMIN`, and the Vue student table adds a confirmed reset action. Login and JWT restoration both require an enabled account.

**Tech Stack:** MySQL 8.4, Flyway, Spring Boot 3.3, Spring Security/BCrypt, Jakarta Validation, Vue 3, Element Plus, Vitest.

---

## File Structure

- Create `backend/src/main/resources/db/migration/V5__repair_user_password_hash.sql`: repair the column and backfill only empty hashes.
- Modify `backend/src/test/java/com/erp/PersistenceBootstrapTest.java`: verify Flyway V5 and the final column contract.
- Modify `backend/src/main/java/com/erp/store/ErpStore.java`: centralize the default password and reset linked accounts.
- Modify `backend/src/main/java/com/erp/service/CompetitionService.java`: expose the store reset result.
- Modify `backend/src/main/java/com/erp/web/CompetitionController.java`: add the reset endpoint and validate student creation.
- Modify `backend/src/main/java/com/erp/dto/CompetitionDtos.java`: validate student fields.
- Modify `backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java`: cover linked-account reset and authorization.
- Modify `backend/src/test/java/com/erp/ErpRealtimeWriteThroughTest.java`: prove every reset account is written through.
- Modify `backend/src/main/java/com/erp/service/AuthService.java`: reject disabled accounts during login.
- Modify `backend/src/main/java/com/erp/security/JwtAuthenticationFilter.java`: reject disabled users when restoring a token.
- Modify `backend/src/main/java/com/erp/dto/AuthDtos.java` and `backend/src/main/java/com/erp/web/AuthController.java`: validate password requests.
- Create `backend/src/test/java/com/erp/AuthAccountStatusIntegrationTest.java`: cover disabled login and token behavior.
- Modify `frontend/src/api/index.ts`: add the reset endpoint contract.
- Modify `frontend/src/views/CompetitionView.vue`: add the reset command, confirmation, and result message.
- Modify `frontend/src/tests/chinese-ui.spec.ts`: cover the frontend reset workflow.
- Modify `README.md`: document `password_hash`, the default password, and reset behavior.

### Task 1: Repair Historical Password Columns

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__repair_user_password_hash.sql`
- Test: `backend/src/test/java/com/erp/PersistenceBootstrapTest.java`

- [ ] **Step 1: Write the failing Flyway version assertion**

Add a test that queries `flyway_schema_history` and expects version `5`, then inspects `information_schema.columns` for `password_hash`, `varchar(100)`, and `IS_NULLABLE = 'NO'`.

```java
@Test
void versionFiveRepairsThePasswordHashColumn() {
    var versionCount = jdbcTemplate.queryForObject(
        "select count(*) from flyway_schema_history where version = '5' and success = 1",
        Long.class
    );
    var column = jdbcTemplate.queryForMap("""
        select data_type, character_maximum_length, is_nullable
        from information_schema.columns
        where table_schema = database()
          and table_name = 'sys_user'
          and column_name = 'password_hash'
        """);

    assertThat(versionCount).isEqualTo(1L);
    assertThat(column.get("data_type")).isEqualTo("varchar");
    assertThat(((Number) column.get("character_maximum_length")).longValue()).isEqualTo(100L);
    assertThat(column.get("is_nullable")).isEqualTo("NO");
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `backend\\mvnw.cmd -f backend/pom.xml -Dtest=PersistenceBootstrapTest#versionFiveRepairsThePasswordHashColumn test`

Expected: FAIL because Flyway version `5` has not been applied.

- [ ] **Step 3: Add the V5 migration**

Use `information_schema.columns` and a prepared statement so both current and historical schemas are supported. Backfill only null/blank values with the verified BCrypt hash for `123456`.

```sql
SET @password_hash_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'password_hash'
);

SET @password_hash_ddl = IF(
    @password_hash_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN password_hash VARCHAR(100) NULL COMMENT ''BCrypt密码摘要'' AFTER username',
    'ALTER TABLE sys_user MODIFY COLUMN password_hash VARCHAR(100) NULL COMMENT ''BCrypt密码摘要'''
);

PREPARE password_hash_stmt FROM @password_hash_ddl;
EXECUTE password_hash_stmt;
DEALLOCATE PREPARE password_hash_stmt;

UPDATE sys_user
SET password_hash = '$2a$10$5mk8apW7ZxEB5vGecJbKiOZxoGTw1Tq98oA4wQQgrJydsSmPYi7oy'
WHERE password_hash IS NULL OR TRIM(password_hash) = '';

ALTER TABLE sys_user
    MODIFY COLUMN password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要';
```

- [ ] **Step 4: Run the migration test and verify GREEN**

Run the Step 2 command again.

Expected: PASS when Docker/Testcontainers is available; otherwise the test is explicitly skipped by the existing test policy and the SQL is verified later against a running MySQL instance.

### Task 2: Reset a Student and Linked ERP Accounts

**Files:**
- Modify: `backend/src/main/java/com/erp/store/ErpStore.java`
- Modify: `backend/src/main/java/com/erp/service/CompetitionService.java`
- Modify: `backend/src/main/java/com/erp/web/CompetitionController.java`
- Test: `backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java`
- Test: `backend/src/test/java/com/erp/ErpRealtimeWriteThroughTest.java`

- [ ] **Step 1: Write failing domain tests**

Add tests that reset `student01`, assert its main account and five `workspaceOwnerId == student.id` accounts match `123456`, assert `student02` keeps its previous hash, and assert a student caller is rejected.

```java
@Test
void superAdminResetsStudentAndEveryWorkspaceAccountToDefaultPassword() {
    var superAdmin = store.userByUsername("superadmin");
    var student = store.userByUsername("student01");
    var unrelatedHash = store.userByUsername("student02").passwordHash;

    var resetCount = store.resetStudentPassword(superAdmin.id, student.id);

    var resetUsers = store.workspaceUsers(student.id);
    assertThat(resetCount).isEqualTo(6);
    assertThat(resetUsers).allSatisfy(user ->
        assertThat(new BCryptPasswordEncoder().matches("123456", user.passwordHash)).isTrue());
    assertThat(store.userByUsername("student02").passwordHash).isEqualTo(unrelatedHash);
}

@Test
void studentCannotResetAnotherStudentPassword() {
    var student01 = store.userByUsername("student01");
    var student02 = store.userByUsername("student02");

    assertThatThrownBy(() -> store.resetStudentPassword(student01.id, student02.id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("权限");
}
```

- [ ] **Step 2: Run the reset tests and verify RED**

Run: `backend\\mvnw.cmd -f backend/pom.xml -Dtest=TrainingCompetitionIntegrationTest test`

Expected: compilation FAIL because `resetStudentPassword` and `workspaceUsers` do not exist.

- [ ] **Step 3: Implement the minimal store operation**

Add `DEFAULT_PASSWORD`, reuse it in seeding/default creation, expose a linked-account view, and update every target through the existing write-through method.

```java
public static final String DEFAULT_PASSWORD = "123456";

public List<User> workspaceUsers(Long studentId) {
    return users.values().stream()
        .filter(user -> user.id.equals(studentId) || Objects.equals(user.workspaceOwnerId, studentId))
        .toList();
}

public int resetStudentPassword(Long operatorId, Long studentId) {
    requireAdmin(userById(operatorId));
    var student = userById(studentId);
    if (student.role != RoleCode.STUDENT || !Objects.equals(student.workspaceOwnerId, student.id)) {
        throw new BusinessException("只能重置测试学员账号密码。");
    }
    var targets = workspaceUsers(student.id);
    targets.forEach(user -> updatePasswordHash(user.id, passwordEncoder.encode(DEFAULT_PASSWORD)));
    return targets.size();
}
```

- [ ] **Step 4: Add service and controller wiring**

```java
public Map<String, Object> resetStudentPassword(String username, Long studentId) {
    var operator = store.userByUsername(username);
    var resetCount = store.resetStudentPassword(operator.id, studentId);
    return Map.of("resetCount", resetCount);
}
```

```java
@PostMapping("/students/{id}/reset-password")
public ApiResult<?> resetStudentPassword(@PathVariable Long id, Authentication authentication) {
    return ApiResult.success(competitionService.resetStudentPassword(authentication.getName(), id));
}
```

- [ ] **Step 5: Verify write-through calls**

Extend `CapturingRealtimeRepository` with `updatedUsers`, invoke the reset, and assert six distinct user IDs were passed to `updateUser`.

- [ ] **Step 6: Run backend reset tests and verify GREEN**

Run: `backend\\mvnw.cmd -f backend/pom.xml -Dtest=TrainingCompetitionIntegrationTest,ErpRealtimeWriteThroughTest test`

Expected: PASS with zero failures.

### Task 3: Enforce Enabled Accounts and Validate Password Requests

**Files:**
- Modify: `backend/src/main/java/com/erp/service/AuthService.java`
- Modify: `backend/src/main/java/com/erp/security/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/erp/dto/AuthDtos.java`
- Modify: `backend/src/main/java/com/erp/dto/CompetitionDtos.java`
- Modify: `backend/src/main/java/com/erp/web/AuthController.java`
- Modify: `backend/src/main/java/com/erp/web/CompetitionController.java`
- Create: `backend/src/test/java/com/erp/AuthAccountStatusIntegrationTest.java`

- [ ] **Step 1: Write failing disabled-account tests**

Use `@InMemoryBusinessTest`, set `student.status = Status.DISABLED`, and verify login throws. Create a real JWT, run `JwtAuthenticationFilter` with Spring mock request/response objects, and verify the security context remains empty.

```java
@Test
void disabledAccountCannotLoginOrRestoreAuthenticationFromAnExistingToken() throws Exception {
    var student = store.userByUsername("student01");
    var token = jwtService.createToken(student.username, student.role.name());
    student.status = Status.DISABLED;

    assertThatThrownBy(() -> authService.login(new LoginRequest("student01", "123456")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("禁用");

    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);
    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
}
```

- [ ] **Step 2: Run the status test and verify RED**

Run: `backend\\mvnw.cmd -f backend/pom.xml -Dtest=AuthAccountStatusIntegrationTest test`

Expected: FAIL because disabled users currently authenticate.

- [ ] **Step 3: Add enabled checks**

In `AuthService.login`, reject `user.status != Status.ENABLED` before matching the password. In `JwtAuthenticationFilter`, only create authentication when the loaded user is enabled; otherwise clear the context.

```java
if (user.status != Status.ENABLED) {
    throw new BusinessException("账号已禁用，请联系管理员。");
}
```

- [ ] **Step 4: Add DTO validation and `@Valid` controller annotations**

Use `@NotBlank` and `@Size(min = 6, max = 72)` for login/change-password values. For optional student passwords, use `@Pattern(regexp = "^$|^.{6,72}$", message = "密码长度应为6到72位")`. Validate `StudentRequest` and `ChangePasswordRequest` at their controller boundaries.

- [ ] **Step 5: Run status and existing context tests**

Run: `backend\\mvnw.cmd -f backend/pom.xml -Dtest=AuthAccountStatusIntegrationTest,ErpApplicationTests test`

Expected: PASS with disabled sessions rejected and the Spring context loading.

### Task 4: Add the Vue Reset Action

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/CompetitionView.vue`
- Test: `frontend/src/tests/chinese-ui.spec.ts`

- [ ] **Step 1: Write the failing frontend source contract test**

```typescript
it('student management can reset the main and ERP account passwords', () => {
  const api = readFileSync(join(process.cwd(), 'src/api/index.ts'), 'utf8')
  const view = readFileSync(join(process.cwd(), 'src/views/CompetitionView.vue'), 'utf8')

  expect(api).toContain('resetStudentPassword')
  expect(api).toContain('/reset-password')
  expect(view).toContain('重置密码')
  expect(view).toContain('主账号及全部 ERP 子账号')
  expect(view).toContain('123456')
})
```

- [ ] **Step 2: Run the frontend test and verify RED**

Run: `npm.cmd test -- --run src/tests/chinese-ui.spec.ts`

Working directory: `frontend`

Expected: FAIL because the API and reset UI do not exist.

- [ ] **Step 3: Add the typed API helper**

```typescript
export function resetStudentPassword(id: number) {
  return http.post(`/competition/students/${id}/reset-password`) as Promise<{ resetCount: number }>
}
```

- [ ] **Step 4: Add the table action and confirmation flow**

Import `Key` from Element Plus icons and `resetStudentPassword` from the API. Expand the fixed action column and add an icon/text button with a tooltip. Implement:

```typescript
async function resetPassword(row: StudentAccount) {
  await ElMessageBox.confirm(
    `确认将学员 ${row.name} 的主账号及全部 ERP 子账号密码重置为 123456？`,
    '重置密码',
    { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await resetStudentPassword(row.id)
  ElMessage.success(`已重置 ${result.resetCount} 个账号，密码为 123456`)
  await load()
}
```

- [ ] **Step 5: Run frontend tests and build**

Run: `npm.cmd test -- --run src/tests/chinese-ui.spec.ts`

Run: `npm.cmd run build`

Working directory: `frontend`

Expected: both commands exit 0 without warnings.

### Task 5: Document and Verify the Complete Flow

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README account documentation**

Document that `sys_user.password_hash` stores BCrypt summaries, built-in accounts default to `123456`, a super administrator can reset a student and all five ERP role accounts together, existing non-empty hashes are preserved by V5, and disabled accounts/tokens are rejected.

- [ ] **Step 2: Run focused backend verification**

Run: `backend\\mvnw.cmd -f backend/pom.xml -Dtest=ErpApplicationTests,TrainingCompetitionIntegrationTest,ErpRealtimeWriteThroughTest,AuthAccountStatusIntegrationTest test`

Expected: all selected tests pass with zero failures and no generated-password or dynamic-agent warnings.

- [ ] **Step 3: Run frontend verification**

Run: `npm.cmd test -- --run`

Run: `npm.cmd run build`

Working directory: `frontend`

Expected: the full Vitest suite and production build exit 0.

- [ ] **Step 4: Inspect the final diff**

Run: `git diff --check`

Run: `git diff -- backend/src/main/resources/db/migration/V5__repair_user_password_hash.sql backend/src/main/java/com/erp/store/ErpStore.java backend/src/main/java/com/erp/service/AuthService.java backend/src/main/java/com/erp/security/JwtAuthenticationFilter.java backend/src/main/java/com/erp/service/CompetitionService.java backend/src/main/java/com/erp/web/CompetitionController.java backend/src/main/java/com/erp/dto/AuthDtos.java backend/src/main/java/com/erp/dto/CompetitionDtos.java frontend/src/api/index.ts frontend/src/views/CompetitionView.vue frontend/src/tests/chinese-ui.spec.ts README.md`

Expected: only the approved account lifecycle changes are present; unrelated dirty-worktree changes remain untouched.
