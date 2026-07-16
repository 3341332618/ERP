package com.erp.service;

import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.Status;
import com.erp.domain.ErpModels.User;
import com.erp.dto.AuthDtos.AvatarRequest;
import com.erp.dto.AuthDtos.ChangePasswordRequest;
import com.erp.dto.AuthDtos.LoginRequest;
import com.erp.security.JwtService;
import com.erp.store.ErpStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
    private final ErpStore store;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(ErpStore store, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.store = store;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> login(LoginRequest request) {
        var user = store.userByUsername(request.username());
        if (user.status != Status.ENABLED) {
            throw new BusinessException("账号已禁用，无法登录！");
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash)) {
            throw new BusinessException("登录密码错误!");
        }
        return Map.of(
            "token", jwtService.createToken(user.username, user.role.name()),
            "user", userView(user),
            "menus", store.menus(user)
        );
    }

    public Map<String, Object> me(String username) {
        var user = store.userByUsername(username);
        return Map.of("user", userView(user), "menus", store.menus(user));
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        var user = store.userByUsername(username);
        if (!passwordEncoder.matches(request.oldPassword(), user.passwordHash)) {
            throw new BusinessException("旧密码错误，请重新输入。");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("确认密码与新密码不一致，请重新输入。");
        }
        store.updatePasswordHash(user.id, passwordEncoder.encode(request.newPassword()));
    }

    public Map<String, Object> avatar(String username, AvatarRequest request) {
        var user = store.userByUsername(username);
        return userView(store.updateAvatar(user.id, request.avatar()));
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
}
