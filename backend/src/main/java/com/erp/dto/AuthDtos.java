package com.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
        @NotBlank(message = "请输入用户名") String username,
        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 72, message = "密码长度应为6到72位")
        String password
    ) {
    }

    public record ChangePasswordRequest(
        @NotBlank(message = "请输入旧密码")
        @Size(min = 6, max = 72, message = "密码长度应为6到72位")
        String oldPassword,
        @NotBlank(message = "请输入新密码")
        @Size(min = 6, max = 72, message = "密码长度应为6到72位")
        String newPassword,
        @NotBlank(message = "请输入确认密码")
        @Size(min = 6, max = 72, message = "密码长度应为6到72位")
        String confirmPassword
    ) {
    }

    public record AvatarRequest(String avatar) {
    }
}