package com.erp;

import com.erp.support.MySqlIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceBootstrapTest extends MySqlIntegrationTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

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

    @Test
    void flywayRecordsSuccessfulVersionOneMigration() {
        var successfulMigrations = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where version = '1' and success = 1",
            Long.class
        );

        assertThat(successfulMigrations).isEqualTo(1L);
    }

    @Test
    void versionFiveRepairsThePasswordHashColumn() {
        var versionCount = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where version = '5' and success = 1",
            Long.class
        );
        var column = jdbcTemplate.queryForMap("""
            select data_type, character_maximum_length, is_nullable
            from information_schema.columns
            where table_schema = database()
              and table_name = 'sys_user'
              and column_name = 'password_hash'
            """);

        assertThat(versionCount).isEqualTo(1L);
        assertThat(column.get("data_type")).isEqualTo("varchar");
        assertThat(((Number) column.get("character_maximum_length")).longValue()).isEqualTo(100L);
        assertThat(column.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void versionFiveAddsAHashColumnMissingFromAHistoricalSchema() {
        var historicalFlyway = flywayAtVersionFour();
        historicalFlyway.clean();
        historicalFlyway.migrate();
        jdbcTemplate.execute("alter table sys_user drop column password_hash");

        currentFlyway().migrate();

        var column = jdbcTemplate.queryForMap("""
            select character_maximum_length, is_nullable
            from information_schema.columns
            where table_schema = database()
              and table_name = 'sys_user'
              and column_name = 'password_hash'
            """);
        assertThat(((Number) column.get("character_maximum_length")).longValue()).isEqualTo(100L);
        assertThat(column.get("is_nullable")).isEqualTo("NO");
    }

    @Test
    void versionFiveBackfillsOnlyBlankHistoricalHashes() {
        var historicalFlyway = flywayAtVersionFour();
        historicalFlyway.clean();
        historicalFlyway.migrate();
        jdbcTemplate.execute("alter table sys_user modify column password_hash varchar(100) null");
        jdbcTemplate.update("""
            insert into erp_workspace (id, code, name, workspace_type, status)
            values (9001, 'HISTORICAL', '历史工作区', 'SYSTEM', 'ENABLED')
            """);
        jdbcTemplate.update("""
            insert into sys_user
                (id, workspace_id, username, password_hash, name, phone, role_code, status)
            values
                (9001, 9001, 'blank_hash_user', '', '空密码用户', '13800009001', 'STUDENT', 'ENABLED'),
                (9002, 9001, 'existing_hash_user', 'existing-hash', '已有密码用户', '13800009002', 'STUDENT', 'ENABLED')
            """);

        currentFlyway().migrate();

        var repairedHash = jdbcTemplate.queryForObject(
            "select password_hash from sys_user where username = 'blank_hash_user'",
            String.class
        );
        var existingHash = jdbcTemplate.queryForObject(
            "select password_hash from sys_user where username = 'existing_hash_user'",
            String.class
        );
        assertThat(new BCryptPasswordEncoder().matches("123456", repairedHash)).isTrue();
        assertThat(existingHash).isEqualTo("existing-hash");
    }

    @Test
    void schemaUsesWorkspaceScopedForeignKeys() {
        var workspaceForeignKeys = jdbcTemplate.queryForList("""
            select concat(table_name, '.', constraint_name, ':',
                group_concat(column_name order by ordinal_position separator ','),
                '->', referenced_table_name, ':',
                group_concat(referenced_column_name order by ordinal_position separator ','))
            from information_schema.key_column_usage
            where table_schema = database()
              and constraint_name in (
                'fk_workspace_owner', 'fk_user_warehouse',
                'fk_product_category', 'fk_product_brand', 'fk_product_unit',
                'fk_document_related', 'fk_document_warehouse',
                'fk_document_target_warehouse', 'fk_document_supplier',
                'fk_document_customer', 'fk_document_creator', 'fk_document_auditor',
                'fk_document_item_document', 'fk_document_item_product',
                'fk_inventory_warehouse', 'fk_inventory_product',
                'fk_settlement_document', 'fk_report_student',
                'fk_file_student', 'fk_ranking_student', 'fk_operation_student'
              )
            group by table_name, constraint_name, referenced_table_name
            """, String.class);
        var workspaceScopedTables = jdbcTemplate.queryForList("""
            select table_name
            from information_schema.columns
            where table_schema = database()
              and column_name = 'workspace_id'
              and table_name in ('biz_document_item', 'test_file_submission', 'test_ranking_history')
            """, String.class);
        var boundaryDeleteRules = jdbcTemplate.queryForList("""
            select concat(constraint_name, ':', delete_rule)
            from information_schema.referential_constraints
            where constraint_schema = database()
              and constraint_name in ('fk_workspace_owner', 'fk_user_warehouse')
            """, String.class);

        assertThat(workspaceForeignKeys).containsExactlyInAnyOrder(
            "erp_workspace.fk_workspace_owner:id,owner_user_id->sys_user:workspace_id,id",
            "sys_user.fk_user_warehouse:workspace_id,warehouse_id->master_warehouse:workspace_id,id",
            "master_product.fk_product_category:workspace_id,category_id->master_category:workspace_id,id",
            "master_product.fk_product_brand:workspace_id,brand_id->master_brand:workspace_id,id",
            "master_product.fk_product_unit:workspace_id,unit_id->master_unit:workspace_id,id",
            "biz_document.fk_document_related:workspace_id,related_document_id->biz_document:workspace_id,id",
            "biz_document.fk_document_warehouse:workspace_id,warehouse_id->master_warehouse:workspace_id,id",
            "biz_document.fk_document_target_warehouse:workspace_id,target_warehouse_id->master_warehouse:workspace_id,id",
            "biz_document.fk_document_supplier:workspace_id,supplier_id->master_supplier:workspace_id,id",
            "biz_document.fk_document_customer:workspace_id,customer_id->master_customer:workspace_id,id",
            "biz_document.fk_document_creator:workspace_id,creator_id->sys_user:workspace_id,id",
            "biz_document.fk_document_auditor:workspace_id,auditor_id->sys_user:workspace_id,id",
            "biz_document_item.fk_document_item_document:workspace_id,document_id->biz_document:workspace_id,id",
            "biz_document_item.fk_document_item_product:workspace_id,product_id->master_product:workspace_id,id",
            "inventory_balance.fk_inventory_warehouse:workspace_id,warehouse_id->master_warehouse:workspace_id,id",
            "inventory_balance.fk_inventory_product:workspace_id,product_id->master_product:workspace_id,id",
            "finance_settlement.fk_settlement_document:workspace_id,document_id->biz_document:workspace_id,id",
            "test_bug_report.fk_report_student:workspace_id,student_id->sys_user:workspace_id,id",
            "test_file_submission.fk_file_student:workspace_id,student_id->sys_user:workspace_id,id",
            "test_ranking_history.fk_ranking_student:workspace_id,student_id->sys_user:workspace_id,id",
            "test_operation_log.fk_operation_student:workspace_id,student_id->sys_user:workspace_id,id"
        );
        assertThat(workspaceScopedTables).containsExactlyInAnyOrder(
            "biz_document_item", "test_file_submission", "test_ranking_history"
        );
        assertThat(boundaryDeleteRules)
            .hasSize(2)
            .noneMatch(rule -> rule.endsWith(":CASCADE"))
            .anyMatch(rule -> rule.startsWith("fk_workspace_owner:"))
            .anyMatch(rule -> rule.startsWith("fk_user_warehouse:"));
    }

    @Test
    void competitionHistoryDoesNotCascadeWhenStudentsAreDeleted() {
        var deleteRules = jdbcTemplate.queryForList("""
            select concat(table_name, ':', delete_rule)
            from information_schema.referential_constraints
            where constraint_schema = database()
              and constraint_name in (
                'fk_report_student', 'fk_file_student',
                'fk_ranking_student', 'fk_operation_student'
              )
            """, String.class);

        assertThat(deleteRules)
            .hasSize(4)
            .allMatch(rule -> !rule.endsWith(":CASCADE"));
    }

    private Flyway flywayAtVersionFour() {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .target(MigrationVersion.fromVersion("4"))
            .load();
    }

    private Flyway currentFlyway() {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();
    }}
