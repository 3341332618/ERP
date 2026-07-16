package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.dto.MasterDataDtos.StatusRequest;
import com.erp.service.MasterDataService;
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
    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping("/{type}")
    public ApiResult<?> list(@PathVariable String type,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             Authentication authentication) {
        return ApiResult.success(masterDataService.list(authentication.getName(), type, keyword, status));
    }

    @PostMapping("/{type}")
    public ApiResult<?> create(@PathVariable String type, @RequestBody Map<String, String> payload, Authentication authentication) {
        return ApiResult.success(masterDataService.create(authentication.getName(), type, payload));
    }

    @PostMapping("/product/import")
    public ApiResult<?> importProducts(@RequestBody List<Map<String, String>> rows, Authentication authentication) {
        return ApiResult.success(masterDataService.importProducts(authentication.getName(), rows));
    }

    @PutMapping("/{type}/{id}")
    public ApiResult<?> update(@PathVariable String type, @PathVariable Long id, @RequestBody Map<String, String> payload, Authentication authentication) {
        return ApiResult.success(masterDataService.update(authentication.getName(), type, id, payload));
    }

    @PatchMapping("/{type}/{id}/status")
    public ApiResult<?> status(@PathVariable String type, @PathVariable Long id, @RequestBody StatusRequest request, Authentication authentication) {
        return ApiResult.success(masterDataService.changeStatus(authentication.getName(), type, id, request));
    }

    @GetMapping("/warehouse-staff")
    public ApiResult<?> warehouseStaff(Authentication authentication) {
        return ApiResult.success(masterDataService.warehouseStaff(authentication.getName()));
    }
}
