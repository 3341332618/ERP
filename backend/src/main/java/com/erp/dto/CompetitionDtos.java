package com.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class CompetitionDtos {
    private CompetitionDtos() {
    }

    public record PublishBugRequest(Boolean active) {
    }

    public record StudentRequest(
        @NotBlank(message = "请输入用户名") String username,
        @NotBlank(message = "请输入姓名") String name,
        @NotBlank(message = "请输入手机号") String phone,
        @Pattern(regexp = "^$|^.{6,72}$", message = "密码长度应为6到72位") String password
    ) {
    }

    public record ReviewRequest(String status, String score, String roundName, String reviewComment) {
    }
}