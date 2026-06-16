package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.store.ErpStore;
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
    public ApiResult<?> list(@PathVariable String direction) {
        return ApiResult.success(store.settlements(direction));
    }
}
