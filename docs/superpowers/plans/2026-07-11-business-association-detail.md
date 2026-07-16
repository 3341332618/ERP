# Business Association Selectors and Details Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace free-form business relationship fields with validated selectors and add master-data and related-document details without changing the existing ERP data model.

**Architecture:** Reuse enabled master-data and stock list APIs for ordinary selectors, add a dedicated backend return-options contract for return documents, and keep the backend as the final authority for workspace, status, type, price, and quantity validation. Put deterministic frontend filtering in a small tested utility module, while the existing Vue views remain responsible for dialog state and user feedback.

**Tech Stack:** Java 17, Spring Boot 3.3, JUnit 5, Vue 3, TypeScript, Element Plus, Vitest.

---

## Working Tree Rule

The current checkout contains relevant uncommitted work in shared backend and frontend files. Work in place, edit only the files listed below, do not reset unrelated changes, and do not create implementation commits that would accidentally include pre-existing user changes. Use `git diff --check` checkpoints instead.

## File Map

- Create `backend/src/main/java/com/erp/dto/DocumentDtos.java`: return-option response records.
- Create `backend/src/test/java/com/erp/BusinessAssociationIntegrationTest.java`: backend relationship, return-option, price, and detail-type tests.
- Modify `backend/src/main/java/com/erp/store/ErpStore.java`: enabled/workspace resolution, return options, return price, detail type validation.
- Modify `backend/src/main/java/com/erp/service/DocumentService.java`: expose return options and typed detail lookup.
- Modify `backend/src/main/java/com/erp/web/DocumentController.java`: add return-options endpoint.
- Create `frontend/src/utils/businessOptions.ts`: pure deduplication and dependent-option functions.
- Create `frontend/src/tests/business-options.spec.ts`: Vitest coverage for the pure functions.
- Modify `frontend/src/api/index.ts`: typed records and detail/return-option wrappers.
- Modify `frontend/src/views/MasterDataView.vue`: product reference selectors, validation, numeric inputs, and master details.
- Modify `frontend/src/views/DocumentView.vue`: business selectors, dependent clearing, return linkage, validation, and source detail.
- Modify `frontend/src/tests/chinese-ui.spec.ts`: source-contract assertions for selectors and detail entry points.

### Task 1: Enforce Existing Enabled Master References

**Files:**
- Create: `backend/src/test/java/com/erp/BusinessAssociationIntegrationTest.java`
- Modify: `backend/src/main/java/com/erp/store/ErpStore.java`

- [ ] **Step 1: Write failing tests for arbitrary and disabled references**

Add `@InMemoryBusinessTest` tests that demonstrate the desired behavior:

```java
@Test
void productReferencesMustAlreadyExistAndBeEnabled() {
    var admin = store.userByUsername("student01_admin");

    assertThatThrownBy(() -> store.createMaster("product", Map.of(
        "name", "任意引用商品",
        "categoryName", "不存在分类",
        "brandName", "不存在品牌",
        "unitName", "不存在单位",
        "purchasePrice", "10",
        "salePrice", "20"
    ), admin.id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("请选择已存在且启用");
}

@Test
void disabledWarehouseAndProductCannotBeUsedByNewDocument() {
    var purchase = store.userByUsername("purchase_staff");
    var warehouse = enabledMaster("warehouse", purchase.id);
    var supplier = enabledMaster("supplier", purchase.id);
    var product = enabledMaster("product", purchase.id);
    store.changeMasterStatus("warehouse", warehouse.id, Status.DISABLED);

    assertThatThrownBy(() -> store.createDocument("purchase-inbound", purchase.id, Map.of(
        "warehouseId", warehouse.id.toString(),
        "partnerId", supplier.id.toString(),
        "productId", product.id.toString(),
        "quantity", "1",
        "price", "10"
    ))).hasMessageContaining("仓库已停用");
}
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=BusinessAssociationIntegrationTest" test
```

Expected: arbitrary names are auto-created or disabled masters are accepted, so the new tests fail for the specified behavior.

- [ ] **Step 3: Implement strict master resolution**

In `ErpStore`:

- Replace the arbitrary-creation branch in `ensureProductReference` with lookup of an enabled local record or enabled system record.
- If a student selects an enabled system record and no local equivalent exists, copy and persist that record into the student workspace.
- Add an enabled association resolver for documents that validates type, workspace visibility, and `Status.ENABLED`, materializing a visible system record to the student workspace when required.
- Use the enabled resolver for warehouse, target warehouse, partner, and product payload fields.
- Keep historical detail reads on the existing visibility-only resolver so approved records remain viewable after a master is disabled.

The failure messages must name the field, for example `仓库已停用，请重新选择。` and `商品分类请选择已存在且启用的数据。`.

- [ ] **Step 4: Run focused and regression tests**

Run:

```powershell
.\mvnw.cmd "-Dtest=BusinessAssociationIntegrationTest,ErpCoreFlowIntegrationTest,TrainingCompetitionIntegrationTest" test
```

Expected: all tests pass, including pre-existing workspace-isolation behavior.

- [ ] **Step 5: Check the scoped diff**

Run `git diff --check -- backend/src/main/java/com/erp/store/ErpStore.java backend/src/test/java/com/erp/BusinessAssociationIntegrationTest.java`.

### Task 2: Return Options, Original Price, and Typed Details

**Files:**
- Create: `backend/src/main/java/com/erp/dto/DocumentDtos.java`
- Modify: `backend/src/main/java/com/erp/store/ErpStore.java`
- Modify: `backend/src/main/java/com/erp/service/DocumentService.java`
- Modify: `backend/src/main/java/com/erp/web/DocumentController.java`
- Test: `backend/src/test/java/com/erp/BusinessAssociationIntegrationTest.java`

- [ ] **Step 1: Add failing return-option tests**

Test these concrete behaviors:

```java
@Test
void returnOptionsOnlyExposeApprovedOwnedSourcesAndRemainingQuantities() {
    var fixture = approvedPurchaseInboundWithPartialReturn();
    var options = store.returnOptions(fixture.purchase().id, "purchase-return", null);

    assertThat(options)
        .filteredOn(option -> option.documentNo().equals(fixture.inbound().documentNo))
        .singleElement()
        .satisfies(option -> {
            assertThat(option.items()).singleElement().satisfies(item -> {
                assertThat(item.originalQuantity()).isEqualByComparingTo("10.00");
                assertThat(item.returnedQuantity()).isEqualByComparingTo("2.00");
                assertThat(item.remainingQuantity()).isEqualByComparingTo("8.00");
            });
        });
}

@Test
void editingReturnOptionExcludesItsOwnQuantity() {
    var fixture = approvedPurchaseInboundWithPartialReturn();

    assertThat(store.returnOptions(fixture.purchase().id, "purchase-return", fixture.currentReturn().id))
        .filteredOn(option -> option.documentNo().equals(fixture.inbound().documentNo))
        .singleElement()
        .satisfies(option -> assertThat(option.items().get(0).remainingQuantity())
            .isEqualByComparingTo("10.00"));
}

@Test
void returnPriceAlwaysComesFromOriginalDocument() {
    var sales = store.userByUsername("sales_staff");
    var outbound = store.createSimpleDocument("sales-outbound", sales.id);
    store.submitDocument(outbound.id, sales.id);
    store.approve(outbound.id, store.userByUsername("warehouse_staff").id);
    var returned = store.createDocument("sales-return", sales.id, Map.of(
        "relatedDocumentNo", outbound.documentNo,
        "productId", outbound.items.get(0).productId.toString(),
        "quantity", "1",
        "price", "0.01"
    ));

    assertThat(returned.items.get(0).price).isEqualByComparingTo(outbound.items.get(0).price);
}

@Test
void documentDetailRejectsMismatchedPathType() {
    var purchase = store.userByUsername("purchase_staff");
    var inbound = store.createSimpleDocument("purchase-inbound", purchase.id);

    assertThatThrownBy(() -> store.documentDetail("sales-outbound", inbound.id, purchase.id))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("单据类型不匹配");
}

private ReturnFixture approvedPurchaseInboundWithPartialReturn() {
    var purchase = store.userByUsername("purchase_staff");
    var inbound = store.createSimpleDocument("purchase-inbound", purchase.id);
    store.submitDocument(inbound.id, purchase.id);
    store.approve(inbound.id, store.userByUsername("warehouse_staff").id);
    var currentReturn = store.createDocument("purchase-return", purchase.id, Map.of(
        "relatedDocumentNo", inbound.documentNo,
        "productId", inbound.items.get(0).productId.toString(),
        "quantity", "2",
        "price", "0.01"
    ));
    return new ReturnFixture(purchase, inbound, currentReturn);
}

private record ReturnFixture(User purchase, DocumentRecord inbound, DocumentRecord currentReturn) {}
```

- [ ] **Step 2: Run tests and verify RED**

Run ` .\mvnw.cmd "-Dtest=BusinessAssociationIntegrationTest" test`.

Expected: compilation fails because `returnOptions` and typed detail methods do not exist.

- [ ] **Step 3: Add response records and backend logic**

Create `DocumentDtos` records matching the approved contract:

```java
public record ReturnDocumentOption(
    Long documentId,
    String documentNo,
    Long warehouseId,
    String warehouseCode,
    String warehouseName,
    Long partnerId,
    String partnerCode,
    String partnerName,
    List<ReturnItemOption> items
) {}

public record ReturnItemOption(
    Long productId,
    String productCode,
    String productName,
    String unitName,
    BigDecimal price,
    BigDecimal originalQuantity,
    BigDecimal returnedQuantity,
    BigDecimal remainingQuantity
) {}
```

Implement `ErpStore.returnOptions(operatorId, type, editingId)` with workspace, role visibility, creator, source type, approved status, positive remaining quantity, and editing-ID ownership/type checks. Derive return item price from the source item in `documentItem`; ignore client return price. Add `documentDetail(type, id, operatorId)` that verifies the requested API type matches the document.

Expose:

```java
@GetMapping("/{type}/return-options")
public ApiResult<?> returnOptions(
    @PathVariable String type,
    @RequestParam(required = false) Long editingId,
    Authentication authentication
)
```

- [ ] **Step 4: Run backend tests**

Run:

```powershell
.\mvnw.cmd "-Dtest=BusinessAssociationIntegrationTest,ErpCoreFlowIntegrationTest" test
```

Expected: all tests pass.

- [ ] **Step 5: Check the scoped diff**

Run `git diff --check` for the five Task 2 files.

### Task 3: Typed Frontend API and Pure Option Functions

**Files:**
- Create: `frontend/src/utils/businessOptions.ts`
- Create: `frontend/src/tests/business-options.spec.ts`
- Modify: `frontend/src/api/index.ts`

- [ ] **Step 1: Write failing Vitest cases**

Cover pure behavior rather than source strings:

```ts
it('deduplicates master options by name and prefers workspace-local records', () => {
  expect(dedupeMasterOptions([
    { id: 1, name: '办公设备', workspaceOwnerId: null, status: 'ENABLED' },
    { id: 2, name: '办公设备', workspaceOwnerId: 100, status: 'ENABLED' }
  ])).toEqual([{ id: 2, name: '办公设备', workspaceOwnerId: 100, status: 'ENABLED' }])
})

it('filters products by selected warehouse and positive available quantity', () => {
  expect(stockProductOptions(stockRows, 10).map((row) => row.productId)).toEqual([30])
})

it('excludes the source warehouse from transfer targets', () => {
  expect(transferTargetOptions(warehouses, 10).map((row) => row.id)).not.toContain(10)
})
```

- [ ] **Step 2: Run tests and verify RED**

Run `npm.cmd test -- --run src/tests/business-options.spec.ts`.

Expected: module import fails because `businessOptions.ts` does not exist.

- [ ] **Step 3: Add API types, wrappers, and helpers**

Add typed interfaces for `MasterRecord`, `DocumentItem`, `DocumentRecord`, `StockRow`, `ReturnDocumentOption`, and `ReturnItemOption`. Add:

```ts
export function documentDetail(type: string, id: number) {
  return http.get(`/documents/${type}/${id}`) as Promise<DocumentRecord>
}

export function listReturnOptions(type: string, editingId?: number) {
  return http.get(`/documents/${type}/return-options`, {
    params: editingId ? { editingId } : undefined
  }) as Promise<ReturnDocumentOption[]>
}
```

Implement and export `dedupeMasterOptions`, `stockProductOptions`, and `transferTargetOptions` as side-effect-free functions.

- [ ] **Step 4: Run tests and typecheck**

Run:

```powershell
npm.cmd test -- --run src/tests/business-options.spec.ts
npm.cmd run build
```

Expected: helper tests and TypeScript build pass.

### Task 4: Master Data Selectors and Detail Dialog

**Files:**
- Modify: `frontend/src/views/MasterDataView.vue`
- Modify: `frontend/src/tests/chinese-ui.spec.ts`

- [ ] **Step 1: Add failing source-contract assertions**

Assert the product form loads enabled category/brand/unit data, uses searchable `el-select` controls without `allow-create`, calls `formRef.validate()`, uses numeric price controls, and contains a `查看详情` action plus a read-only details dialog.

- [ ] **Step 2: Run the target test and verify RED**

Run `npm.cmd test -- --run src/tests/chinese-ui.spec.ts`.

Expected: assertions fail on missing selectors/details.

- [ ] **Step 3: Implement product selectors and master details**

- Load enabled category, brand, and unit lists together when `type === 'product'`.
- Deduplicate them with `dedupeMasterOptions` and use `name` as the select value.
- Add `FormInstance` ref and validate before create/update.
- Replace purchase/sale price text fields with `el-input-number` using `min=0.01`, `precision=2`, and `step=1`.
- Add a `View` icon action that opens a read-only `el-dialog`; display common fields and type-specific product/contact fields.
- When editing a product whose current reference is no longer enabled, display the old value as disabled and require a new enabled choice before save.

- [ ] **Step 4: Run tests and build**

Run:

```powershell
npm.cmd test -- --run src/tests/chinese-ui.spec.ts
npm.cmd run build
```

Expected: tests and build pass.

### Task 5: Document Selectors, Return Linkage, and Source Detail

**Files:**
- Modify: `frontend/src/views/DocumentView.vue`
- Modify: `frontend/src/tests/chinese-ui.spec.ts`

- [ ] **Step 1: Add failing source-contract assertions**

Assert that warehouse, partner, product, target warehouse, and return source use searchable selects; return warehouse/partner are read-only; quantity/price use input-number; option changes clear dependent fields; and the detail dialog has `查看原单`.

- [ ] **Step 2: Run target tests and verify RED**

Run `npm.cmd test -- --run src/tests/chinese-ui.spec.ts src/tests/business-options.spec.ts`.

Expected: the new DocumentView assertions fail.

- [ ] **Step 3: Implement selector loading and linkage**

- On dialog open, load enabled warehouses, the correct partner type, enabled products, and stock rows in parallel.
- For returns, load `listReturnOptions(config.api, editingId)` instead and make source document the first required control.
- Clear product on warehouse change; exclude the source warehouse from targets.
- For sales outbound and transfer, use `stockProductOptions` for the selected warehouse.
- On source return change, fill warehouse/partner display fields, clear product/quantity, and expose only source items with positive remaining quantity.
- On product change, populate suggestion/original price. Return price is read-only; ordinary price remains editable.
- Use `el-input-number` for quantity and price, with return quantity max bound to `remainingQuantity`.
- Add `FormInstance` ref and validate before save; keep the dialog open on API errors and refresh candidate lists.

- [ ] **Step 4: Implement related source detail**

In the return detail dialog, make `relatedDocumentNo` actionable. Resolve its source type (`purchase-inbound` or `sales-outbound`), find the source ID from its visible document list, call `documentDetail`, and show a second read-only detail dialog with header, items, and operation information. If the source no longer exists, show `关联原单不可用或无权查看。`.

- [ ] **Step 5: Run all frontend verification**

Run:

```powershell
npm.cmd test -- --run
npm.cmd run build
```

Expected: all Vitest tests pass and Vite production build succeeds.

### Task 6: Final Verification and Review

**Files:** All files listed in the File Map.

- [ ] **Step 1: Run complete default backend tests**

Run `.\mvnw.cmd test` from `backend`.

Expected: build success, zero failures, zero skipped tests, and no default Docker warning.

- [ ] **Step 2: Run complete frontend tests and build**

Run `npm.cmd test -- --run` and `npm.cmd run build` from `frontend`.

Expected: all tests and production build pass.

- [ ] **Step 3: Check diffs and whitespace**

Run `git diff --check` and inspect only the task files. Confirm no unrelated changes were reverted or staged.

- [ ] **Step 4: Review residual MySQL coverage**

If Docker Engine is available, run `.\mvnw.cmd -Pmysql-integration test`. If unavailable, report the real-MySQL test gap explicitly; do not weaken or skip the profile.

- [ ] **Step 5: Dispatch final spec and code-quality reviews**

The final review must confirm every approved design requirement and report Critical/Important/Minor findings before completion.
