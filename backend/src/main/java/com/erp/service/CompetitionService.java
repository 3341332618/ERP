package com.erp.service;

import com.erp.common.BusinessException;
import com.erp.dto.CompetitionDtos.PublishBugRequest;
import com.erp.dto.CompetitionDtos.ReviewRequest;
import com.erp.dto.CompetitionDtos.StudentRequest;
import com.erp.store.ErpStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CompetitionService {
    private final ErpStore store;

    public CompetitionService(ErpStore store) {
        this.store = store;
    }

    public Object bugs(String username) {
        var user = store.userByUsername(username);
        return store.bugDefinitions(user.id);
    }

    public Object students(String username) {
        var user = store.userByUsername(username);
        return store.students(user.id);
    }

    public Object createStudent(String username, StudentRequest request) {
        var user = store.userByUsername(username);
        var payload = new LinkedHashMap<String, String>();
        payload.put("username", request.username());
        payload.put("name", request.name());
        payload.put("phone", request.phone());
        if (request.password() != null && !request.password().isBlank()) {
            payload.put("password", request.password());
        }
        return store.createStudent(user.id, payload);
    }

    public Map<String, Integer> resetStudentPassword(String username, Long studentId) {
        var user = store.userByUsername(username);
        var resetCount = store.resetStudentPassword(user.id, studentId);
        return Map.of("resetCount", resetCount);
    }

    public void deleteStudent(String username, Long studentId) {
        var user = store.userByUsername(username);
        store.deleteStudent(user.id, studentId);
    }

    public Object publish(String username, String bugId, PublishBugRequest request) {
        var user = store.userByUsername(username);
        return store.publishBug(bugId, Boolean.TRUE.equals(request.active()), user.id);
    }

    public Object tasks(String username) {
        var user = store.userByUsername(username);
        return store.activeBugTasks(user.id);
    }

    public Object reports(String username) {
        var user = store.userByUsername(username);
        return store.bugReports(user.id);
    }

    public Object submitReport(String username, Map<String, ?> payload) {
        var user = store.userByUsername(username);
        return store.submitBugReport(user.id, ServicePayloads.stringMap(payload));
    }

    public Object reviewReport(String username, Long id, ReviewRequest request) {
        var user = store.userByUsername(username);
        return store.reviewBugReport(user.id, id, reviewPayload(request));
    }

    public Object files(String username) {
        var user = store.userByUsername(username);
        return store.competitionFiles(user.id);
    }

    public Object submitFile(String username, String title, String moduleName, String bugId, MultipartFile file) {
        var user = store.userByUsername(username);
        var storagePath = saveCompetitionFile(user.username, file);
        return store.submitCompetitionFile(user.id, Map.of(
            "title", title,
            "moduleName", moduleName,
            "bugId", bugId == null ? "" : bugId,
            "fileName", safeFileName(file),
            "contentType", file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
            "fileSize", String.valueOf(file.getSize()),
            "storagePath", storagePath
        ));
    }

    public Object reviewFile(String username, Long id, ReviewRequest request) {
        var user = store.userByUsername(username);
        return store.reviewCompetitionFile(user.id, id, reviewPayload(request));
    }

    public Object logs(String username, Long studentId) {
        var user = store.userByUsername(username);
        return store.studentOperationLogs(user.id, studentId);
    }

    public Object history(String username) {
        var user = store.userByUsername(username);
        return store.rankingHistory(user.id);
    }

    public Object rankings() {
        return store.bugRankings();
    }

    private Map<String, String> reviewPayload(ReviewRequest request) {
        var payload = new LinkedHashMap<String, String>();
        if (request.status() != null) {
            payload.put("status", request.status());
        }
        if (request.score() != null) {
            payload.put("score", request.score());
        }
        if (request.roundName() != null) {
            payload.put("roundName", request.roundName());
        }
        if (request.reviewComment() != null) {
            payload.put("reviewComment", request.reviewComment());
        }
        return payload;
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
}
