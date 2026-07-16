package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.service.SystemService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/messages")
    public ApiResult<?> messages(Authentication authentication) {
        return ApiResult.success(systemService.messages(authentication.getName()));
    }
}
