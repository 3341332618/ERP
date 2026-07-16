package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.service.DocumentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{type}")
    public ApiResult<?> list(@PathVariable String type, Authentication authentication) {
        return ApiResult.success(documentService.list(authentication.getName(), type));
    }

    @GetMapping("/{type}/return-options")
    public ApiResult<?> returnOptions(@PathVariable String type,
                                      @RequestParam(required = false) Long editingId,
                                      Authentication authentication) {
        return ApiResult.success(documentService.returnOptions(authentication.getName(), type, editingId));
    }

    @PostMapping("/{type}")
    public ApiResult<?> create(@PathVariable String type,
                               @RequestBody(required = false) Map<String, Object> payload,
                               Authentication authentication) {
        return ApiResult.success(documentService.create(authentication.getName(), type, payload));
    }

    @PutMapping("/{type}/{id}")
    public ApiResult<?> update(@PathVariable String type,
                               @PathVariable Long id,
                               @RequestBody(required = false) Map<String, Object> payload,
                               Authentication authentication) {
        return ApiResult.success(documentService.update(authentication.getName(), type, id, payload));
    }

    @GetMapping("/{type}/{id}")
    public ApiResult<?> detail(@PathVariable String type, @PathVariable Long id, Authentication authentication) {
        return ApiResult.success(documentService.detail(authentication.getName(), type, id));
    }

    @PostMapping("/{type}/{id}/submit")
    public ApiResult<?> submit(@PathVariable String type, @PathVariable Long id, Authentication authentication) {
        return ApiResult.success(documentService.submit(authentication.getName(), id));
    }

    @DeleteMapping("/{type}/{id}")
    public ApiResult<?> delete(@PathVariable String type, @PathVariable Long id, Authentication authentication) {
        return ApiResult.success(documentService.delete(authentication.getName(), id));
    }
}
