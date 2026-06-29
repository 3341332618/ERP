package com.erp.web;

import com.erp.common.ApiResult;
import com.erp.common.BusinessException;
import com.erp.store.ErpStore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/competition")
public class CompetitionController {
    private final ErpStore store;

    public CompetitionController(ErpStore store) {
        this.store = store;
    }

    @GetMapping("/bugs")
    public ApiResult<?> bugs() {
        return ApiResult.success(store.bugDefinitions());
    }

    @GetMapping("/students")
    public ApiResult<?> students(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.students(user.id));
    }

    @PostMapping("/students")
    public ApiResult<?> createStudent(@RequestBody Map<String, ?> payload, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.createStudent(user.id, stringMap(payload)));
    }

    @DeleteMapping("/students/{id}")
    public ApiResult<?> deleteStudent(@PathVariable Long id, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        store.deleteStudent(user.id, id);
        return ApiResult.success();
    }

    @PatchMapping("/bugs/{id}/publish")
    public ApiResult<?> publish(@PathVariable String id,
                                @RequestBody Map<String, ?> payload,
                                Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.publishBug(id, Boolean.parseBoolean(String.valueOf(payload.get("active"))), user.id));
    }

    @GetMapping("/tasks")
    public ApiResult<?> tasks(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.activeBugTasks(user.id));
    }

    @GetMapping("/reports")
    public ApiResult<?> reports(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.bugReports(user.id));
    }

    @PostMapping("/reports")
    public ApiResult<?> submitReport(@RequestBody Map<String, ?> payload, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.submitBugReport(user.id, stringMap(payload)));
    }

    @PatchMapping("/reports/{id}/review")
    public ApiResult<?> reviewReport(@PathVariable Long id,
                                     @RequestBody Map<String, ?> payload,
                                     Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.reviewBugReport(user.id, id, stringMap(payload)));
    }

    @GetMapping("/files")
    public ApiResult<?> files(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.competitionFiles(user.id));
    }

    @PostMapping("/files")
    public ApiResult<?> submitFile(@RequestParam String title,
                                   @RequestParam String moduleName,
                                   @RequestParam(required = false, defaultValue = "") String bugId,
                                   @RequestParam MultipartFile file,
                                   Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        var storagePath = saveCompetitionFile(user.username, file);
        return ApiResult.success(store.submitCompetitionFile(user.id, Map.of(
            "title", title,
            "moduleName", moduleName,
            "bugId", bugId,
            "fileName", safeFileName(file),
            "contentType", file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
            "fileSize", String.valueOf(file.getSize()),
            "storagePath", storagePath
        )));
    }

    @PatchMapping("/files/{id}/review")
    public ApiResult<?> reviewFile(@PathVariable Long id,
                                   @RequestBody Map<String, ?> payload,
                                   Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.reviewCompetitionFile(user.id, id, stringMap(payload)));
    }

    @GetMapping("/logs")
    public ApiResult<?> logs(@RequestParam(required = false) Long studentId, Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.studentOperationLogs(user.id, studentId));
    }

    @GetMapping("/history")
    public ApiResult<?> history(Authentication authentication) {
        var user = store.userByUsername(authentication.getName());
        return ApiResult.success(store.rankingHistory(user.id));
    }

    @GetMapping("/rankings")
    public ApiResult<?> rankings() {
        return ApiResult.success(store.bugRankings());
    }

    private String saveCompetitionFile(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传PDF、Word或Excel测试文件。");
        }
        var safeName = safeFileName(file);
        if (!validCompetitionFile(safeName)) {
            throw new BusinessException("仅支持PDF、Word、Excel文件，请重新上传。");
        }
        try {
            var dir = Path.of("uploads", "competition", username);
            Files.createDirectories(dir);
            var target = dir.resolve(System.currentTimeMillis() + "-" + safeName);
            file.transferTo(target);
            return target.toString();
        } catch (IOException ex) {
            throw new BusinessException("测试文件保存失败，请重新上传。");
        }
    }

    private String safeFileName(MultipartFile file) {
        var original = file.getOriginalFilename();
        var fallback = "测试文件";
        var normalized = original == null ? "" : original.replace("\\", "/");
        var slashIndex = normalized.lastIndexOf('/');
        var fileName = normalized.isBlank() ? fallback : normalized.substring(slashIndex + 1);
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private boolean validCompetitionFile(String fileName) {
        var lower = fileName.toLowerCase();
        return lower.endsWith(".pdf")
            || lower.endsWith(".doc")
            || lower.endsWith(".docx")
            || lower.endsWith(".xls")
            || lower.endsWith(".xlsx");
    }

    private Map<String, String> stringMap(Map<String, ?> payload) {
        return payload.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() == null ? "" : String.valueOf(entry.getValue())
            ));
    }
}
