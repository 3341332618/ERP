package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.store.ErpStore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final ErpStore store;

    public DocumentController(ErpStore store) {
        this.store = store;
    }

    @GetMapping("/{type}")
    public ApiResult<?> list(@PathVariable String type, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.documents(type, user.id));
    }

    @PostMapping("/{type}")
    public ApiResult<?> create(@PathVariable String type,
                               @RequestBody(required = false) Map<String, Object> payload,
                               Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.createDocument(type, user.id, payload == null ? Map.of() : payload));
    }

    @PutMapping("/{type}/{id}")
    public ApiResult<?> update(@PathVariable String type,
                               @PathVariable Long id,
                               @RequestBody(required = false) Map<String, Object> payload,
                               Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.updateDocument(type, id, user.id, payload == null ? Map.of() : payload));
    }

    @GetMapping("/{type}/{id}")
    public ApiResult<?> detail(@PathVariable String type, @PathVariable Long id) {
        return ApiResult.success(store.getDocument(id));
    }

    @PostMapping("/{type}/{id}/submit")
    public ApiResult<?> submit(@PathVariable String type, @PathVariable Long id, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.submitDocument(id, user.id));
    }

    @DeleteMapping("/{type}/{id}")
    public ApiResult<?> delete(@PathVariable String type, @PathVariable Long id, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.deleteDocument(id, user.id));
    }
}
