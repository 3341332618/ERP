package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.service.SettlementService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlement")
public class SettlementController {
    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/{direction}")
    public ApiResult<?> list(@PathVariable String direction, Authentication authentication) {
        return ApiResult.success(settlementService.list(authentication.getName(), direction));
    }

    @GetMapping("/{direction}/{id}")
    public ApiResult<?> detail(@PathVariable String direction, @PathVariable Long id, Authentication authentication) {
        return ApiResult.success(settlementService.detail(authentication.getName(), direction, id));
    }
}
