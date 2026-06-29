package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.domain.ErpModels.RoleCode;
import com.erp.domain.ErpModels.Status;
import com.erp.store.ErpStore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masterdata")
public class MasterDataController {
    private final ErpStore store;

    public MasterDataController(ErpStore store) {
        this.store = store;
    }

    @GetMapping("/{type}")
    public ApiResult<?> list(@PathVariable String type,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.masters(type, keyword, status, user.id));
    }

    @PostMapping("/{type}")
    public ApiResult<?> create(@PathVariable String type, @RequestBody Map<String, String> payload, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.createMaster(type, payload, user.id));
    }

    @PostMapping("/product/import")
    public ApiResult<?> importProducts(@RequestBody List<Map<String, String>> rows, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.importProducts(rows, user.id));
    }

    @PutMapping("/{type}/{id}")
    public ApiResult<?> update(@PathVariable String type, @PathVariable Long id, @RequestBody Map<String, String> payload, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.updateMaster(type, id, payload, user.id));
    }

    @PatchMapping("/{type}/{id}/status")
    public ApiResult<?> status(@PathVariable String type, @PathVariable Long id, @RequestBody Map<String, String> payload, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.changeMasterStatus(type, id, Status.valueOf(payload.get("status")), user.id));
    }

    @GetMapping("/warehouse-staff")
    public ApiResult<?> warehouseStaff() {
        return ApiResult.success(store.usersByRole(RoleCode.WAREHOUSE_STAFF));
    }
}
