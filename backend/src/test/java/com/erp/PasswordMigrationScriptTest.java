package com.erp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordMigrationScriptTest {
    @Test
    void migrationRepairsMissingAndBlankPasswordHashesWithoutOverwritingExistingValues() throws IOException {
        try (var stream = getClass().getResourceAsStream("/db/migration/V5__repair_user_password_hash.sql")) {
            assertThat(stream).isNotNull();
            var sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                .contains("information_schema.columns")
                .contains("ADD COLUMN password_hash VARCHAR(100) NULL")
                .contains("WHERE password_hash IS NULL OR TRIM(password_hash) = ''")
                .contains("MODIFY COLUMN password_hash VARCHAR(100) NOT NULL")
                .doesNotContain("UPDATE sys_user SET password_hash = '$2a$10$5mk8apW7ZxEB5vGecJbKiOZxoGTw1Tq98oA4wQQgrJydsSmPYi7oy';");
        }
    }
}