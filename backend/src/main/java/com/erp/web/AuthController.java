package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.User;
import com.erp.security.JwtService;
import com.erp.store.ErpStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ErpStore store;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(ErpStore store, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.store = store;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        var user = store.userByUsername(request.username());
        if (!passwordEncoder.matches(request.password(), user.passwordHash)) {
            throw new BusinessException("登录密码错误!");
        }
        return ApiResult.success(Map.of(
            "token", jwtService.createToken(user.username, user.role.name()),
            "user", userView(user),
            "menus", store.menus(user)
        ));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(Map.of("user", userView(user), "menus", store.menus(user)));
    }

    @PostMapping("/change-password")
    public ApiResult<Void> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        var user = store.userByUsername(authentication.getName());
        if (!passwordEncoder.matches(request.oldPassword(), user.passwordHash)) {
            throw new BusinessException("旧密码错误，请重新输入。");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("确认密码与新密码不一致，请重新输入。");
        }
        user.passwordHash = passwordEncoder.encode(request.newPassword());
        return ApiResult.success();
    }

    @PostMapping("/avatar")
    public ApiResult<Map<String, Object>> avatar(Authentication authentication, @RequestBody AvatarRequest request) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(userView(store.updateAvatar(user.id, request.avatar())));
    }

    private Map<String, Object> userView(User user) {
        return Map.of(
            "id", user.id,
            "username", user.username,
            "name", user.name,
            "phone", user.phone,
            "avatar", user.avatar == null ? "" : user.avatar,
            "role", user.role.name(),
            "roleName", user.role.label,
            "createTime", user.createTime.toString()
        );
    }

    public record LoginRequest(
        @NotBlank(message = "请输入用户名") String username,
        @NotBlank(message = "请输入密码") String password
    ) {
    }

    public record ChangePasswordRequest(String oldPassword, String newPassword, String confirmPassword) {
    }

    public record AvatarRequest(String avatar) {
    }
}
