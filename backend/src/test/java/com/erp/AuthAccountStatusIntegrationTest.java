package com.erp;

import com.erp.common.BusinessException;
import com.erp.domain.ErpModels.Status;
import com.erp.dto.AuthDtos.ChangePasswordRequest;
import com.erp.dto.AuthDtos.LoginRequest;
import com.erp.dto.CompetitionDtos.StudentRequest;
import com.erp.security.JwtAuthenticationFilter;
import com.erp.security.JwtService;
import com.erp.service.AuthService;
import com.erp.store.ErpStore;
import com.erp.support.InMemoryBusinessTest;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@InMemoryBusinessTest
class AuthAccountStatusIntegrationTest {
    @Autowired
    ErpStore store;

    @Autowired
    AuthService authService;

    @Autowired
    JwtService jwtService;

    @Autowired
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    Validator validator;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledStudentCannotLoginWithCorrectPassword() {
        var student = store.userByUsername("student01");
        student.status = Status.DISABLED;

        assertThatThrownBy(() -> authService.login(
            new LoginRequest("student01", ErpStore.DEFAULT_PASSWORD)
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁用");
    }

    @Test
    void tokenIssuedBeforeStudentWasDisabledDoesNotAuthenticate() throws Exception {
        var student = store.userByUsername("student01");
        var token = jwtService.createToken(student.username, student.role.name());
        student.status = Status.DISABLED;
        SecurityContextHolder.clearContext();

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        jwtAuthenticationFilter.doFilter(
            request,
            new MockHttpServletResponse(),
            new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void loginRequestRequiresUsernameAndPasswordBetweenSixAndSeventyTwoCharacters() {
        assertThat(violationProperties(new LoginRequest("", "123456"))).contains("username");
        assertThat(violationProperties(new LoginRequest("student01", "12345"))).contains("password");
        assertThat(validator.validate(new LoginRequest("student01", "123456"))).isEmpty();
        assertThat(validator.validate(new LoginRequest("student01", "1".repeat(72)))).isEmpty();
    }

    @Test
    void changePasswordRequestRequiresNonBlankPasswordsBetweenSixAndSeventyTwoCharacters() {
        assertThat(violationProperties(new ChangePasswordRequest("", "", "")))
            .containsExactlyInAnyOrder("oldPassword", "newPassword", "confirmPassword");
        assertThat(violationProperties(new ChangePasswordRequest("12345", "12345", "12345")))
            .containsExactlyInAnyOrder("oldPassword", "newPassword", "confirmPassword");
        assertThat(validator.validate(new ChangePasswordRequest("123456", "123456", "123456"))).isEmpty();

        var maximumLengthPassword = "1".repeat(72);
        assertThat(validator.validate(new ChangePasswordRequest(
            maximumLengthPassword,
            maximumLengthPassword,
            maximumLengthPassword
        ))).isEmpty();
    }

    @Test
    void studentRequestRequiresIdentityFieldsAndAllowsOnlyBlankOrValidLengthPassword() {
        assertThat(violationProperties(new StudentRequest("", "", "", "")))
            .containsExactlyInAnyOrder("username", "name", "phone");
        assertThat(validator.validate(new StudentRequest("student03", "学员三", "13900000003", ""))).isEmpty();
        assertThat(validator.validate(new StudentRequest("student03", "学员三", "13900000003", null))).isEmpty();
        assertThat(violationProperties(
            new StudentRequest("student03", "学员三", "13900000003", "12345")
        )).containsExactly("password");
        assertThat(validator.validate(
            new StudentRequest("student03", "学员三", "13900000003", "123456")
        )).isEmpty();
    }

    private Set<String> violationProperties(Object request) {
        return validator.validate(request).stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());
    }
}