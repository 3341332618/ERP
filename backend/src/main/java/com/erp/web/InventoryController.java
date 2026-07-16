package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.dto.InventoryDtos.RejectRequest;
import com.erp.service.InventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stock")
    public ApiResult<?> stock(@RequestParam(required = false) Long editingId,
                              Authentication authentication) {
        return ApiResult.success(inventoryService.stock(authentication.getName(), editingId));
    }

    @GetMapping("/audit/{direction}")
    public ApiResult<?> auditList(@PathVariable String direction, Authentication authentication) {
        return ApiResult.success(inventoryService.auditList(authentication.getName(), direction));
    }

    @PostMapping("/audit/{id}/approve")
    public ApiResult<?> approve(@PathVariable Long id, Authentication authentication) {
        return ApiResult.success(inventoryService.approve(authentication.getName(), id));
    }

    @PostMapping("/audit/{id}/reject")
    public ApiResult<?> reject(@PathVariable Long id, Authentication authentication, @RequestBody RejectRequest request) {
        return ApiResult.success(inventoryService.reject(authentication.getName(), id, request));
    }
}
