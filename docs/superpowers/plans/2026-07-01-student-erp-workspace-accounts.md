# Student ERP Workspace Accounts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give each testing student a dedicated ERP workspace with automatically generated role-specific ERP subaccounts.

**Architecture:** Extend the existing `workspaceOwnerId` model to users. Student main accounts own workspaces; generated ERP subaccounts point to the owning student and reuse existing workspace filtering, document, stock, settlement, and operation-log logic.

**Tech Stack:** Spring Boot 3, Java 17, JUnit 5, AssertJ, Vue 3, TypeScript, Vitest.

---

## File Structure

- Modify `backend/src/main/java/com/erp/domain/ErpModels.java`
  - Add `User.workspaceOwnerId`.
  - Add `StudentWorkspaceAccount`.
  - Add `StudentAccount.erpAccounts`.
- Modify `backend/src/main/java/com/erp/store/ErpStore.java`
  - Generate student ERP subaccounts.
  - Resolve workspace owner for student subaccounts.
  - Restrict student main menu to testing competition.
  - Filter warehouse staff lists and notifications by workspace.
  - Delete student subaccounts and workspace business data.
- Modify `backend/src/main/java/com/erp/web/MasterDataController.java`
  - Pass current user to warehouse-staff listing.
- Modify `backend/src/main/java/com/erp/web/AuthController.java`
  - No endpoint shape change is required; menu output changes through store logic.
- Modify `backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java`
  - Add regression tests for student subaccount generation, menu separation, workspace flow, and deletion cleanup.
- Modify `frontend/src/api/index.ts`
  - Add `StudentWorkspaceAccount` type and expose `erpAccounts` on `StudentAccount`.
- Modify `frontend/src/views/CompetitionView.vue`
  - Show generated ERP child accounts on the student management page.
- Modify `frontend/src/tests/chinese-ui.spec.ts`
  - Assert the student management UI contains ERP subaccount copy.
- Modify `README.md`, `README.en.md`, `docs/operation-manual.md`
  - Document the new login and testing workflow.

## Task 1: Backend account model and generation

**Files:**
- Modify: `backend/src/main/java/com/erp/domain/ErpModels.java`
- Modify: `backend/src/main/java/com/erp/store/ErpStore.java`
- Test: `backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Add this test to `TrainingCompetitionIntegrationTest`:

```java
@Test
void creatingStudentAutomaticallyCreatesRoleSpecificErpAccounts() {
    var admin = store.userByUsername("admin");

    var created = store.createStudent(admin.id, Map.of(
        "username", "student_auto",
        "name", "自动学员",
        "phone", "13900000999",
        "password", "654321"
    ));

    assertThat(created.erpAccounts)
        .extracting(account -> account.username)
        .containsExactlyInAnyOrder(
            "student_auto_purchase_staff",
            "student_auto_warehouse_staff",
            "student_auto_sales_staff",
            "student_auto_settlement_manager"
        );
    assertThat(store.userByUsername("student_auto_purchase_staff").role).isEqualTo(RoleCode.PURCHASE_STAFF);
    assertThat(store.userByUsername("student_auto_warehouse_staff").role).isEqualTo(RoleCode.WAREHOUSE_STAFF);
    assertThat(store.userByUsername("student_auto_sales_staff").role).isEqualTo(RoleCode.SALES_STAFF);
    assertThat(store.userByUsername("student_auto_settlement_manager").role).isEqualTo(RoleCode.SETTLEMENT_MANAGER);
    assertThat(store.userByUsername("student_auto_purchase_staff").workspaceOwnerId).isEqualTo(created.id);
    assertThat(store.userByUsername("student_auto_warehouse_staff").workspaceOwnerId).isEqualTo(created.id);
    assertThat(store.userByUsername("student_auto_sales_staff").workspaceOwnerId).isEqualTo(created.id);
    assertThat(store.userByUsername("student_auto_settlement_manager").workspaceOwnerId).isEqualTo(created.id);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=TrainingCompetitionIntegrationTest#creatingStudentAutomaticallyCreatesRoleSpecificErpAccounts test
```

Expected: compilation failure because `erpAccounts` and `workspaceOwnerId` do not exist, or assertion failure because subaccounts are not generated.

- [ ] **Step 3: Add domain fields**

In `ErpModels.User`, add:

```java
public Long workspaceOwnerId;
```

In `ErpModels`, add:

```java
public static class StudentWorkspaceAccount {
    public Long id;
    public String username;
    public String name;
    public RoleCode role;
    public String roleName;
}
```

In `ErpModels.StudentAccount`, add:

```java
public List<StudentWorkspaceAccount> erpAccounts = new ArrayList<>();
```

- [ ] **Step 4: Generate student workspace accounts**

In `ErpStore`, add a helper that creates the student main account and four subaccounts. Use suffixes `_purchase_staff`, `_warehouse_staff`, `_sales_staff`, `_settlement_manager`; set child `workspaceOwnerId` to the main student id. Update seed and `createStudent` to call this helper.

- [ ] **Step 5: Verify test passes**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=TrainingCompetitionIntegrationTest#creatingStudentAutomaticallyCreatesRoleSpecificErpAccounts test
```

Expected: test passes.

## Task 2: Menu and workspace ownership behavior

**Files:**
- Modify: `backend/src/main/java/com/erp/store/ErpStore.java`
- Test: `backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Add this test:

```java
@Test
void studentMainAccountUsesCompetitionOnlyAndErpSubaccountsUseOwnedWorkspace() {
    var student = store.userByUsername("student01");
    var purchase = store.userByUsername("student01_purchase_staff");
    var warehouse = store.userByUsername("student01_warehouse_staff");
    var settlement = store.userByUsername("student01_settlement_manager");

    assertThat(store.menus(student.role))
        .extracting(menu -> menu.title)
        .containsExactly("测试竞赛");
    assertThat(store.menus(purchase.role))
        .extracting(menu -> menu.title)
        .containsExactly("采购管理");
    assertThat(store.menus(warehouse.role))
        .extracting(menu -> menu.title)
        .containsExactly("库存管理");
    assertThat(store.menus(settlement.role))
        .extracting(menu -> menu.title)
        .containsExactly("结算管理");

    var inbound = store.createSimpleDocument("purchase-inbound", purchase.id);
    assertThat(inbound.workspaceOwnerId).isEqualTo(student.id);
    store.submitDocument(inbound.id, purchase.id);

    assertThat(store.auditList("inbound", store.userByUsername("warehouse_staff").id))
        .extracting(document -> document.documentNo)
        .doesNotContain(inbound.documentNo);
    assertThat(store.auditList("inbound", warehouse.id))
        .extracting(document -> document.documentNo)
        .contains(inbound.documentNo);

    store.approve(inbound.id, warehouse.id);

    assertThat(store.stockViews(warehouse.id)).isNotEmpty();
    assertThat(store.settlements("expense", settlement.id))
        .extracting(record -> record.relatedDocumentNo)
        .contains(inbound.documentNo);
    assertThat(store.studentOperationLogs(store.userByUsername("superadmin").id, student.id))
        .extracting(log -> log.moduleName)
        .contains("采购入库");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=TrainingCompetitionIntegrationTest#studentMainAccountUsesCompetitionOnlyAndErpSubaccountsUseOwnedWorkspace test
```

Expected: failure because student still has ERP menus or child workspace ownership is not used.

- [ ] **Step 3: Update workspace owner resolution**

Change `workspaceOwnerId(Long userId)` to return:

```java
if (user.role == RoleCode.STUDENT) {
    return user.id;
}
return user.workspaceOwnerId;
```

- [ ] **Step 4: Restrict student main menu**

In `menus(RoleCode role)`, keep `RoleCode.STUDENT` mapped only to `competitionStudentMenu()`. ERP subaccounts already use their business role menus.

- [ ] **Step 5: Prefer workspace private master data**

Update `first(String type, Long ownerId)` so exact workspace records are preferred before global records. This ensures child ERP accounts use their own warehouse when a private warehouse exists.

- [ ] **Step 6: Filter audit and notifications by workspace**

Update `auditList`, `documentWarehouseMatches`, `documentAuditWarehouseMatches`, and `notifyWarehouse` so platform warehouse staff only receive platform documents, while student warehouse staff receive documents where `document.workspaceOwnerId` equals their `workspaceOwnerId`.

- [ ] **Step 7: Verify test passes**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=TrainingCompetitionIntegrationTest#studentMainAccountUsesCompetitionOnlyAndErpSubaccountsUseOwnedWorkspace test
```

Expected: test passes.

## Task 3: Delete cleanup and warehouse-staff API filtering

**Files:**
- Modify: `backend/src/main/java/com/erp/store/ErpStore.java`
- Modify: `backend/src/main/java/com/erp/web/MasterDataController.java`
- Test: `backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Add this test:

```java
@Test
void deletingStudentRemovesSubaccountsAndWorkspaceData() {
    var admin = store.userByUsername("admin");
    var created = store.createStudent(admin.id, Map.of(
        "username", "student_delete",
        "name", "删除学员",
        "phone", "13900000888"
    ));
    var purchase = store.userByUsername("student_delete_purchase_staff");
    var warehouse = store.userByUsername("student_delete_warehouse_staff");

    var inbound = store.createSimpleDocument("purchase-inbound", purchase.id);
    store.submitDocument(inbound.id, purchase.id);
    store.approve(inbound.id, warehouse.id);
    assertThat(store.stockViews(warehouse.id)).isNotEmpty();

    store.deleteStudent(admin.id, created.id);

    assertThatThrownBy(() -> store.userByUsername("student_delete"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("登录账号不存在");
    assertThatThrownBy(() -> store.userByUsername("student_delete_purchase_staff"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("登录账号不存在");
    assertThat(store.bugRankings())
        .extracting(row -> row.get("studentName"))
        .doesNotContain("删除学员");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=TrainingCompetitionIntegrationTest#deletingStudentRemovesSubaccountsAndWorkspaceData test
```

Expected: failure because subaccounts or workspace data are still present.

- [ ] **Step 3: Implement cleanup**

Update `deleteStudent` to remove users whose `id` equals the student id or whose `workspaceOwnerId` equals the student id. Also remove master records, documents, stocks, settlements, messages, reports, files, logs, and ranking records for that workspace.

- [ ] **Step 4: Filter warehouse staff options**

Change `usersByRole(RoleCode role)` to `usersByRole(RoleCode role, Long userId)` and filter by the caller’s workspace owner. Update `MasterDataController.warehouseStaff` to receive `Authentication`, resolve current user, and pass `user.id`.

- [ ] **Step 5: Verify test passes**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=TrainingCompetitionIntegrationTest#deletingStudentRemovesSubaccountsAndWorkspaceData test
```

Expected: test passes.

## Task 4: Frontend display and documentation

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/views/CompetitionView.vue`
- Modify: `frontend/src/tests/chinese-ui.spec.ts`
- Modify: `README.md`
- Modify: `README.en.md`
- Modify: `docs/operation-manual.md`

- [ ] **Step 1: Write the failing frontend test**

In `frontend/src/tests/chinese-ui.spec.ts`, extend the competition UI test to assert:

```ts
expect(competitionView).toContain('ERP子账号')
expect(competitionView).toContain('采购专员')
expect(competitionView).toContain('仓库专员')
expect(competitionView).toContain('销售专员')
expect(competitionView).toContain('结算主管')
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
cd frontend
npm.cmd test -- --run src/tests/chinese-ui.spec.ts
```

Expected: test fails because the UI does not show ERP subaccounts.

- [ ] **Step 3: Update API types**

Add:

```ts
export interface StudentWorkspaceAccount {
  id: number
  username: string
  name: string
  role: string
  roleName: string
}
```

Update `StudentAccount`:

```ts
erpAccounts: StudentWorkspaceAccount[]
```

- [ ] **Step 4: Render ERP subaccounts**

In the student table in `CompetitionView.vue`, add a column titled `ERP子账号` that renders each child account username and role label.

- [ ] **Step 5: Update docs**

Document that each student has a main testing account plus generated ERP role accounts. Include `student01_purchase_staff`, `student01_warehouse_staff`, `student01_sales_staff`, and `student01_settlement_manager` in README and operation manual.

- [ ] **Step 6: Verify frontend test passes**

Run:

```powershell
cd frontend
npm.cmd test -- --run src/tests/chinese-ui.spec.ts
```

Expected: test passes.

## Task 5: Full verification and commit

**Files:**
- All modified files from previous tasks.

- [ ] **Step 1: Run targeted backend tests**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=TrainingCompetitionIntegrationTest,ErpCoreFlowIntegrationTest test
```

Expected: all selected backend tests pass.

- [ ] **Step 2: Run frontend test and build**

Run:

```powershell
cd frontend
npm.cmd test -- --run src/tests/chinese-ui.spec.ts
npm.cmd run build
```

Expected: selected frontend test passes and production build exits with code 0.

- [ ] **Step 3: Check stale wording**

Run:

```powershell
rg -n "学生一个账号|学员一个账号|全能 ERP|直接拥有完整 ERP" README.md README.en.md docs frontend/src backend/src
```

Expected: no stale wording that contradicts the new model.

- [ ] **Step 4: Commit**

Run:

```powershell
git add backend/src/main/java/com/erp/domain/ErpModels.java backend/src/main/java/com/erp/store/ErpStore.java backend/src/main/java/com/erp/web/MasterDataController.java backend/src/test/java/com/erp/TrainingCompetitionIntegrationTest.java frontend/src/api/index.ts frontend/src/views/CompetitionView.vue frontend/src/tests/chinese-ui.spec.ts README.md README.en.md docs/operation-manual.md
git commit -m "Add student ERP workspace subaccounts"
```

Expected: commit succeeds with only intended files staged.

## Self-Review

- Spec coverage: account generation, menus, workspace ownership, warehouse filtering, deletion cleanup, frontend display, and docs are all mapped to tasks.
- Placeholder scan: no unfinished marker or open-ended implementation steps remain.
- Type consistency: `workspaceOwnerId`, `StudentWorkspaceAccount`, and `erpAccounts` are consistently named across backend, API type, and UI tasks.
