package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.store.ErpStore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final ErpStore store;

    public InventoryController(ErpStore store) {
        this.store = store;
    }

    @GetMapping("/stock")
    public ApiResult<?> stock(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.stockViews(user.id));
    }

    @GetMapping("/audit/{direction}")
    public ApiResult<?> auditList(@PathVariable String direction, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.auditList(direction, user.id));
    }

    @PostMapping("/audit/{id}/approve")
    public ApiResult<?> approve(@PathVariable Long id, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.approve(id, user.id));
    }

    @PostMapping("/audit/{id}/reject")
    public ApiResult<?> reject(@PathVariable Long id, Authentication authentication, @RequestBody Map<String, String> payload) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.reject(id, user.id, payload.get("reason")));
    }
}
