package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.dto.CompetitionDtos.PublishBugRequest;
import com.erp.dto.CompetitionDtos.ReviewRequest;
import com.erp.dto.CompetitionDtos.StudentRequest;
import com.erp.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/competition")
public class CompetitionController {
    private final CompetitionService competitionService;

    public CompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping("/bugs")
    public ApiResult<?> bugs(Authentication authentication) {
        return ApiResult.success(competitionService.bugs(authentication.getName()));
    }

    @GetMapping("/students")
    public ApiResult<?> students(Authentication authentication) {
        return ApiResult.success(competitionService.students(authentication.getName()));
    }

    @PostMapping("/students")
    public ApiResult<?> createStudent(
        @Valid @RequestBody StudentRequest request,
        Authentication authentication
    ) {
        return ApiResult.success(competitionService.createStudent(authentication.getName(), request));
    }

    @PostMapping("/students/{id}/reset-password")
    public ApiResult<?> resetStudentPassword(@PathVariable Long id, Authentication authentication) {
        return ApiResult.success(competitionService.resetStudentPassword(authentication.getName(), id));
    }

    @DeleteMapping("/students/{id}")
    public ApiResult<?> deleteStudent(@PathVariable Long id, Authentication authentication) {
        competitionService.deleteStudent(authentication.getName(), id);
        return ApiResult.success();
    }

    @PatchMapping("/bugs/{id}/publish")
    public ApiResult<?> publish(@PathVariable String id,
                                @RequestBody PublishBugRequest request,
                                Authentication authentication) {
        return ApiResult.success(competitionService.publish(authentication.getName(), id, request));
    }

    @GetMapping("/tasks")
    public ApiResult<?> tasks(Authentication authentication) {
        return ApiResult.success(competitionService.tasks(authentication.getName()));
    }

    @GetMapping("/reports")
    public ApiResult<?> reports(Authentication authentication) {
        return ApiResult.success(competitionService.reports(authentication.getName()));
    }

    @PostMapping("/reports")
    public ApiResult<?> submitReport(@RequestBody Map<String, ?> payload, Authentication authentication) {
        return ApiResult.success(competitionService.submitReport(authentication.getName(), payload));
    }

    @PatchMapping("/reports/{id}/review")
    public ApiResult<?> reviewReport(@PathVariable Long id,
                                     @RequestBody ReviewRequest request,
                                     Authentication authentication) {
        return ApiResult.success(competitionService.reviewReport(authentication.getName(), id, request));
    }

    @GetMapping("/files")
    public ApiResult<?> files(Authentication authentication) {
        return ApiResult.success(competitionService.files(authentication.getName()));
    }

    @PostMapping("/files")
    public ApiResult<?> submitFile(@RequestParam String title,
                                   @RequestParam String moduleName,
                                   @RequestParam(required = false, defaultValue = "") String bugId,
                                   @RequestParam MultipartFile file,
                                   Authentication authentication) {
        return ApiResult.success(competitionService.submitFile(authentication.getName(), title, moduleName, bugId, file));
    }

    @PatchMapping("/files/{id}/review")
    public ApiResult<?> reviewFile(@PathVariable Long id,
                                   @RequestBody ReviewRequest request,
                                   Authentication authentication) {
        return ApiResult.success(competitionService.reviewFile(authentication.getName(), id, request));
    }

    @GetMapping("/logs")
    public ApiResult<?> logs(@RequestParam(required = false) Long studentId, Authentication authentication) {
        return ApiResult.success(competitionService.logs(authentication.getName(), studentId));
    }

    @GetMapping("/history")
    public ApiResult<?> history(Authentication authentication) {
        return ApiResult.success(competitionService.history(authentication.getName()));
    }

    @GetMapping("/rankings")
    public ApiResult<?> rankings() {
        return ApiResult.success(competitionService.rankings());
    }
}
