package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.store.ErpStore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlement")
public class SettlementController {
    private final ErpStore store;

    public SettlementController(ErpStore store) {
        this.store = store;
    }

    @GetMapping("/{direction}")
    public ApiResult<?> list(@PathVariable String direction, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.settlements(direction, user.id));
    }

    @GetMapping("/{direction}/{id}")
    public ApiResult<?> detail(@PathVariable String direction, @PathVariable Long id, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.settlementDetail(direction, id, user.id));
    }
}
