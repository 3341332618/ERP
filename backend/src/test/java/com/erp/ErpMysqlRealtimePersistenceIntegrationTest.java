package com.erp;

import com.erp.domain.ErpModels.BugReportStatus;
import com.erp.store.ErpStore;
import com.erp.store.JdbcErpRealtimeRepository;
import com.erp.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErpMysqlRealtimePersistenceIntegrationTest extends MySqlIntegrationTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcErpRealtimeRepository repository;

    @Test
    void businessOperationsWriteThroughToMysqlTablesImmediately() {
        var store = new ErpStore(new BCryptPasswordEncoder(), repository);
        var admin = store.userByUsername("superadmin");
        var student = store.userByUsername("student01");
        var bugId = store.bugDefinitions(admin.id).get(0).id;

        store.publishBug(bugId, true, admin.id);
        var brand = store.createMaster("brand", Map.of("name", "实时测试品牌"), student.id);
        var report = store.submitBugReport(student.id, Map.of(
            "moduleName", "库存管理",
            "title", "审核后库存数量异常",
            "reproduceSteps", "创建入库单并审核",
            "expectedResult", "库存数量正确增加",
            "actualResult", "库存数量没有变化",
            "evidence", "截图说明"
        ));
        store.reviewBugReport(admin.id, report.id, Map.of(
            "status", BugReportStatus.APPROVED.name(),
            "score", "88",
            "reviewComment", "有效缺陷"
        ));

        var active = jdbcTemplate.queryForObject(
            "select active from test_bug_definition where id = ?",
            Boolean.class,
            bugId
        );
        var brandCount = jdbcTemplate.queryForObject(
            "select count(*) from master_brand where workspace_id = ? and id = ? and name = ?",
            Long.class,
            student.id,
            brand.id,
            "实时测试品牌"
        );
        var reportStatus = jdbcTemplate.queryForObject(
            "select report_status from test_bug_report where id = ?",
            String.class,
            report.id
        );
        var reportScore = jdbcTemplate.queryForObject(
            "select score from test_bug_report where id = ?",
            Integer.class,
            report.id
        );

        assertThat(active).isTrue();
        assertThat(brandCount).isEqualTo(1L);
        assertThat(reportStatus).isEqualTo("APPROVED");
        assertThat(reportScore).isEqualTo(88);
    }
}
