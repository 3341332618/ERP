package com.erp;

import com.erp.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceBootstrapTest extends MySqlIntegrationTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void startsWithMySql() {
        var product = jdbcTemplate.queryForObject("select @@version_comment", String.class);

        assertThat(product).containsIgnoringCase("MySQL");
    }

    @Test
    void flywayCreatesCoreTables() {
        var tables = jdbcTemplate.queryForList(
            "select table_name from information_schema.tables where table_schema = database()",
            String.class
        );
        assertThat(tables).contains(
            "erp_workspace", "sys_user", "master_product", "biz_document",
            "biz_document_item", "inventory_balance", "finance_settlement",
            "test_bug_report", "test_file_submission"
        );
    }
}
