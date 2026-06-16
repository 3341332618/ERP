package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.store.ErpStore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final ErpStore store;

    public SystemController(ErpStore store) {
        this.store = store;
    }

    @GetMapping("/messages")
    public ApiResult<?> messages(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.messages(user.id));
    }
}

