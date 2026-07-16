package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.dto.AuthDtos.AvatarRequest;
import com.erp.dto.AuthDtos.ChangePasswordRequest;
import com.erp.dto.AuthDtos.LoginRequest;
import com.erp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(Authentication authentication) {
        return ApiResult.success(authService.me(authentication.getName()));
    }

    @PostMapping("/change-password")
    public ApiResult<Void> changePassword(
        Authentication authentication,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(authentication.getName(), request);
        return ApiResult.success();
    }

    @PostMapping("/avatar")
    public ApiResult<Map<String, Object>> avatar(Authentication authentication, @RequestBody AvatarRequest request) {
        return ApiResult.success(authService.avatar(authentication.getName(), request));
    }
}
